function startAutomaticOrder() {
    const formData = serializeForm('automatic-form')
    requestApi('post', '/automatic-order', formData)
        .then((response) => {
            if(response.statusText === 'OK'){
                window.alert("Automatic Order started successfully!");
            }
        })
        .catch(error=>console.log(error));
}
function stopAutomaticOrder() {
}

document.getElementById('btn-start-automatic-trade').addEventListener('click', function (ev) {
    ev.preventDefault();
    startAutomaticOrder();
})