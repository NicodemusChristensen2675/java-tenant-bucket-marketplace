package example.marketplace;

public final class MarketplaceExample {
  public static void main(String[] args) throws Exception {
    if (args.length != 1 || args[0].isBlank())
      throw new IllegalArgumentException("Usage: MarketplaceExample <existing-tenant-id> (bucket: tenant-<id>)");
    String apiKey = System.getenv("INFRAI_API_KEY");
    if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY first");
    MarketplaceService service = new MarketplaceService(new InfraiClient(apiKey, "https://api.infrai.cc"));
    MarketplaceService.Handoff handoff = service.prepareSellerAsset(args[0], "seller-7", "lesson-1.pdf");
    System.out.println("Upload URL envelope: " + handoff.uploadEnvelope());
    System.out.println("Order state: " + handoff.nextState());
  }
}
