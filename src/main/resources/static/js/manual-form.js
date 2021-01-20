function startManualOrder(orderPosition) {
    const formData = serializeForm('manual-form')

    const prefixForForm = `manual-${orderPosition}`

    const formDataForOrderPosition = {}

    for (const [key, value] of Object.entries(formData)) {
        if(key.startsWith(prefixForForm)){
            formDataForOrderPosition[key] = value;
        }
    }

    console.log(formDataForOrderPosition)

    requestApi('post', '/manual-order', formDataForOrderPosition)
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