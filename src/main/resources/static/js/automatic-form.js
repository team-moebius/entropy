function startAutomaticOrder(market) {
    const formData = serializeForm('automatic-form')
    requestApi('post', `${market}/order/automatic`, formData)
        .then((response) => {
            const responseElement = document.getElementById("response-info");
            const responseTextElement = document.getElementById("text-response");

            if (response.statusText === 'OK'){
                responseTextElement.textContent = "Automatic Order started successfully!";
                responseElement.style.display = "block";
                setTimeout(function() {responseElement.style.display = "none";}, 5000);
            }
        })
        .catch(error=>console.log(error));
}
function stopAutomaticOrder(market) {
    requestApi('delete', `${market}/order/automatic`)
        .then((response) => {
            const responseElement = document.getElementById("response-info");
            const responseTextElement = document.getElementById("text-response");

            if (response.statusText === 'OK'){
              responseTextElement.textContent = "Automatic Order has been cancelled!";
              responseElement.style.display = "block";
              setTimeout(function() {responseElement.style.display = "none";}, 5000);
            }
        })
        .catch(error=>console.log(error));
}

document.getElementById('btn-start-automatic-trade').addEventListener('click', function (ev) {
    ev.preventDefault();
    startAutomaticOrder();
})

document.getElementById('btn-stop-automatic-trade').addEventListener('click', function (ev) {
    ev.preventDefault();
    stopAutomaticOrder();
})