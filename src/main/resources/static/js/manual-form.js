function startManualOrder(orderPosition) {
    const formData = serializeForm(`manual-${orderPosition}-form`);

    requestApi('post', '/order/manual', formData)
        .then((response) => {
            const responseElement = document.getElementById("response-info");
            const responseTextElement = document.getElementById("text-response");

            if (response.statusText === 'OK') {
                responseTextElement.textContent = "Manual Order started successfully!";
                responseElement.style.display = "block";
                setTimeout(function() {responseElement.style.display = "none";}, 5000);
            }
        })
        .catch(error => console.log(error));
}

document.getElementById('btn-manual-sell-start').addEventListener('click', function (ev) {
    ev.preventDefault();
    startManualOrder("sell");
})

document.getElementById('btn-manual-buy-start').addEventListener('click', function (ev) {
    ev.preventDefault();
    startManualOrder("buy");
})