window.addEventListener('DOMContentLoaded', () => {
    const market = document.getElementById("market");
    const symbolElements = document.getElementsByClassName("text-symbol");
    const priceElement = document.getElementById("text-price");
    const priceUnitElement = document.getElementById("text-price-unit");

    const eventSource = new EventSource(`${market.value}/subscribe-market-prices`);
    eventSource.onmessage = function(sse){
        const marketPriceDto = JSON.parse(sse.data);
        const tradeCurrency = marketPriceDto.tradeCurrency;
        for (let symbolElement of symbolElements) {
            symbolElement.textContent = marketPriceDto.symbol;
        }
        priceElement.textContent = `${marketPriceDto.price}${tradeCurrency}`;
        priceUnitElement.textContent = `${marketPriceDto.priceUnit}${tradeCurrency}`;
        document.title = `${marketPriceDto.symbol} - ${marketPriceDto.price}${tradeCurrency}`;
    }
});
