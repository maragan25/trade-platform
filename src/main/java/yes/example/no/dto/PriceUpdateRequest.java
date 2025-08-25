package yes.example.no.dto;

public class PriceUpdateRequest {
    private int symbolId;
    private double bidPrice;
    private double askPrice;
    private long timestamp;
    
    // Constructors
    public PriceUpdateRequest() {}
    
    public PriceUpdateRequest(int symbolId, double bidPrice, double askPrice, long timestamp) {
        this.symbolId = symbolId;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public int getSymbolId() { return symbolId; }
    public void setSymbolId(int symbolId) { this.symbolId = symbolId; }
    
    public double getBidPrice() { return bidPrice; }
    public void setBidPrice(double bidPrice) { this.bidPrice = bidPrice; }
    
    public double getAskPrice() { return askPrice; }
    public void setAskPrice(double askPrice) { this.askPrice = askPrice; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}