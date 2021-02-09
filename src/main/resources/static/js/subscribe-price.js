window.addEventListener('DOMContentLoaded', () => {
    const symbolElement = document.getElementById("text-symbol");
    const priceElement = document.getElementById("text-price");
    const priceUnitElement = document.getElementById("text-price-unit");

    const eventSource = new EventSource('subscribe-market-prices');
    eventSource.onmessage = function(sse){
        const marketPriceDto = JSON.parse(sse.data);
        const tradeCurrency = marketPriceDto.tradeCurrency;
        symbolElement.textContent = marketPriceDto.symbol;
        priceElement.textContent = `${marketPriceDto.price}${tradeCurrency}`;
        priceUnitElement.textContent = `${marketPriceDto.priceUnit}${tradeCurrency}`;
    }
});
