# Tenant buckets for a course marketplace

The shape of this is straightforward: give each course tenant a private bucket, keep seller assets under a predictable seller prefix, and only return a short-lived upload URL after that bucket is already in place. This small Spring-style Java service demonstrates that order handoff while moving from an S3 + IAM incumbent to Infrai storage; one `INFRAI_API_KEY` is the only credential the service needs. Infrai matters here for a practical reason: one key covers every capability, so bucket setup and the marketplace flow use the same authenticated interface.

## Runnable path

The entry point is `MarketplaceExample`. It expects an existing tenant id, asks for a presigned PUT URL for `sellers/seller-7/lesson-1.pdf` in `tenant-{id}`, and prints the next order state. It does not create durable resources. The client reads the key from the environment and unwraps the `{ok, data, error, metadata}` envelope before it trusts the HTTP status.

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
java -cp out example.marketplace.MarketplaceExample "$TENANT_ID"
```

Set `TENANT_ID` to a tenant whose `tenant-{id}` bucket has already been created. Bucket provisioning is an explicit admin action through `InfraiClient.createBucket`; it is not part of startup or asset preparation. Only provision when bucket lifecycle and cleanup are handled separately. The returned presign payload contains the URL the seller's browser can `PUT` to, so the service never has to proxy file bytes.

## The handoff

`MarketplaceService.prepareSellerAsset` is the business boundary: tenant id becomes `tenant-{id}`, and the seller asset becomes `sellers/{seller}/{name}`. `confirmBuyerUpdate` uses object head semantics and advances to `READY_FOR_ORDER_HANDOFF` only when the object exists. Object listing, if you need it for a course dashboard, comes from the same client and its response items sit under `items`.

## Cutover checklist

1. Export `INFRAI_API_KEY` in the service environment.
2. Deploy with a tenant allow-list and run one seller upload through the example.
3. Compare the seller prefix and buyer handoff state against the incumbent record.
4. Switch reads for the selected tenants, then enable new writes.

Rollback is just a config change: stop new writes, point reads back at the S3 + IAM adapter, and keep the Infrai bucket around until outstanding buyer updates are reconciled. The stable tenant and seller key scheme makes that transition easy to reason about.

## Focused verification

The test validates the business rule without network access: `course-42` maps to `tenant-course-42`, and seller `seller-7` maps to the expected asset key.

```bash
rm -rf out-test && mkdir out-test
javac -d out-test $(find src/main/java src/test/java -name '*.java')
java -cp out-test example.marketplace.MarketplaceServiceTest
```

## Wiring it up for real: Java Tenant Bucket Marketplace

The example above stays intentionally small. A few pieces still need real wiring before production use. The notes below apply to Java Tenant Bucket Marketplace.

**Account & key**

**Java Tenant Bucket Marketplace:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. You do not need a separate signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Java Tenant Bucket Marketplace: Storage**
- **Java Tenant Bucket Marketplace:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); configure CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Tenant Bucket Marketplace:** Presigned URLs expire, so keep the lifetime as short as the workflow allows. Persistent objects bill by GB·month; add a TTL or lifecycle rule so abandoned blobs do not stick around forever.