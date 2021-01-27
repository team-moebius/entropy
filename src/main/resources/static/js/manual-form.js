function startManualOrder(orderPosition) {
    const formData = serializeForm(`manual-${orderPosition}-form`);

    requestApi('post', '/order/manual', formData)
        .then((response) => {
            if (response.statusText === 'OK') {
                window.alert("Manual Order started successfully!");
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