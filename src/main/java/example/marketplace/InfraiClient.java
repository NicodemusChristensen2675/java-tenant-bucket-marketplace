package example.marketplace;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.regex.*;

public final class InfraiClient {
  // Canonical storage.object.presign usage is kept visible at this boundary.
  private final HttpClient http = HttpClient.newHttpClient();
  private final String key;
  private final String baseUrl;

  public InfraiClient(String key, String baseUrl) { this.key = key; this.baseUrl = baseUrl; }

  public String createBucket(String name) throws Exception {
    return call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + esc(name) + "\"}");
  }
  public String presign(String bucket, String objectKey, String op) throws Exception {
    return call("POST", "/v1/storage/object/presign/" + enc(bucket) + "/" + enc(objectKey),
        "{\"op\":\"" + esc(op) + "\",\"expires_seconds\":600}");
  }
  public String head(String bucket, String objectKey) throws Exception {
    return call("GET", "/v1/storage/object/head/" + enc(bucket) + "/" + enc(objectKey), null);
  }
  public String list(String bucket) throws Exception {
    return call("GET", "/v1/storage/object/list/" + enc(bucket), null);
  }

  private String call(String method, String path, String body) throws Exception {
    for (int attempt = 0; attempt < 4; attempt++) {
      HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20))
          .header("Authorization", "Bearer " + key).header("Content-Type", "application/json");
      if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody());
      else b.method(method, HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
      String json = res.body();
      if (json.contains("\"ok\":false")) throw new IllegalStateException("Infrai request rejected: " + json);
      if (res.statusCode() == 429) { Thread.sleep(500L * (1L << attempt)); continue; }
      if (res.statusCode() >= 500) throw new IllegalStateException("Infrai transport failure: " + res.statusCode());
      return json;
    }
    throw new IllegalStateException("Infrai request rate limited");
  }
  private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
  private static String enc(String s) { return s.replace(" ", "%20").replace("/", "%2F"); }
}
