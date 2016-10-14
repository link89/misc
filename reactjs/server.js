var express = require('express');
var app = express();

// respond with "hello world" when a GET request is made to the homepage
app.get('/', function(req, res) {
  res.send('hello world');
});

app.listen('8888', '127.0.0.1', function(err) {
  if (err) {
    console.log(err);
    return;
  }

});
