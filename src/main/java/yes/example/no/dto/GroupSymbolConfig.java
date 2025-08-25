package yes.example.no.dto;

public class GroupSymbolConfig {
    private int symbolId;
    private boolean canViewQuotes;
    private boolean canTrade;
    private double bidMarkup;
    private double askMarkup;
    private double bidMarkupPercent;
    private double askMarkupPercent;
    
    // Constructors
    public GroupSymbolConfig() {}
    
    // Getters and Setters
    public int getSymbolId() { return symbolId; }
    public void setSymbolId(int symbolId) { this.symbolId = symbolId; }
    
    public boolean isCanViewQuotes() { return canViewQuotes; }
    public void setCanViewQuotes(boolean canViewQuotes) { this.canViewQuotes = canViewQuotes; }
    
    public boolean isCanTrade() { return canTrade; }
    public void setCanTrade(boolean canTrade) { this.canTrade = canTrade; }
    
    public double getBidMarkup() { return bidMarkup; }
    public void setBidMarkup(double bidMarkup) { this.bidMarkup = bidMarkup; }
    
    public double getAskMarkup() { return askMarkup; }
    public void setAskMarkup(double askMarkup) { this.askMarkup = askMarkup; }
    
    public double getBidMarkupPercent() { return bidMarkupPercent; }
    public void setBidMarkupPercent(double bidMarkupPercent) { this.bidMarkupPercent = bidMarkupPercent; }
    
    public double getAskMarkupPercent() { return askMarkupPercent; }
    public void setAskMarkupPercent(double askMarkupPercent) { this.askMarkupPercent = askMarkupPercent; }
}