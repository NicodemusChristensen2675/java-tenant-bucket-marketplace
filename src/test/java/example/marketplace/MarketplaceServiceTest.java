package example.marketplace;

public final class MarketplaceServiceTest {
  public static void main(String[] args) throws Exception {
    if (!"tenant-course-42".equals(MarketplaceService.bucketFor("course-42"))) throw new AssertionError("tenant bucket decision changed");
    if (!"sellers/seller-7/lesson-1.pdf".equals(MarketplaceService.assetKey("seller-7", "lesson-1.pdf"))) throw new AssertionError("asset key mismatch");
    System.out.println("PASS tenant bucket and seller asset decision");
  }
}
