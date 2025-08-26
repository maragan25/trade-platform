package yes.example.no.dto;

import yes.example.no.dto.SharedDTOs.SymbolDto;

public class GroupSymbolDto {
    private Long id;
    private SymbolDto symbol;
    private boolean canViewQuotes;
    private boolean canTrade;
    private double bidMarkup;
    private double askMarkup;
    private double bidMarkupPercent;
    private double askMarkupPercent;
    
    public GroupSymbolDto() {}
    
    public GroupSymbolDto(Long id, SymbolDto symbol, boolean canViewQuotes, 
                         boolean canTrade, double bidMarkup, double askMarkup, 
                         double bidMarkupPercent, double askMarkupPercent) {
        this.id = id;
        this.symbol = symbol;
        this.canViewQuotes = canViewQuotes;
        this.canTrade = canTrade;
        this.bidMarkup = bidMarkup;
        this.askMarkup = askMarkup;
        this.bidMarkupPercent = bidMarkupPercent;
        this.askMarkupPercent = askMarkupPercent;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SymbolDto getSymbol() { return symbol; }
    public void setSymbol(SymbolDto symbol) { this.symbol = symbol; }
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
