import axios from 'axios';

setInterval(function() {
  axios.get("/time")
    .then(function(response) {
      document.getElementById("placeholder").innerText = "From the server: " + response.data.value;
    })
    .catch(function (error) {
      console.log(error);
    });
}, 2000);
