package example.marketplace;

public final class MarketplaceService {
  private final InfraiClient storage;
  public MarketplaceService(InfraiClient storage) { this.storage = storage; }
  public static String bucketFor(String tenantId) { return "tenant-" + tenantId; }
  public static String assetKey(String sellerId, String assetName) { return "sellers/" + sellerId + "/" + assetName; }

  public Handoff prepareSellerAsset(String tenantId, String sellerId, String assetName) throws Exception {
    String bucket = bucketFor(tenantId);
    String key = assetKey(sellerId, assetName);
    String uploadEnvelope = storage.presign(bucket, key, "put");
    return new Handoff(bucket, key, uploadEnvelope, "buyer-review");
  }

  public String confirmBuyerUpdate(Handoff handoff) throws Exception {
    String head = storage.head(handoff.bucket(), handoff.key());
    if (head.contains("\"found\":true")) return "READY_FOR_ORDER_HANDOFF";
    return "WAITING_FOR_SELLER_ASSET";
  }

  public record Handoff(String bucket, String key, String uploadEnvelope, String nextState) {}
}
