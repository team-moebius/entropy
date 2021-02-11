function startAutomaticOrder() {
    const formData = serializeForm('automatic-form')
    requestApi('post', '/order/automatic', formData)
        .then((response) => {
            if(response.statusText === 'OK'){
                window.alert("Automatic Order started successfully!");
            }
        })
        .catch(error=>console.log(error));
}
function stopAutomaticOrder() {
    requestApi('delete', '/order/automatic')
        .then((response) => {
            if(response.statusText === 'OK'){
                window.alert("Automatic Order has been cancelled!");
            }
        })
        .catch(error=>console.log(error));
}

document.getElementById('btn-start-automatic-trade').addEventListener('click', function (ev) {
    ev.preventDefault();
    startAutomaticOrder();
})