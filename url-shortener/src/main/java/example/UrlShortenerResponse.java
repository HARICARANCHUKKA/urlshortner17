package example;

public class UrlShortenerResponse {
    private String shortUrl;

    public UrlShortenerResponse(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }
}
