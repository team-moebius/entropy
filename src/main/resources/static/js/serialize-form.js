function serializeForm(formId){
    const targetForm = new FormData(document.getElementById(formId));
    const formData = {}
    for (let entry of targetForm.entries()){
        formData[entry[0]] = entry[1]
    }
    return formData;
}

const requestApi = (method, path, body={}) => axios({
    method, url: path, data: body
})