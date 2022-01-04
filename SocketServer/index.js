let app = require('express')();
let server = require('http').createServer(app);
let io = require('socket.io')(server);

io.on('connection', (socket) => {
  // console.log(socket);

  socket.on('disconnect', function(){
    console.log('who leave : ' + socket.username);
    // io.emit('users-changed', {user: socket.username, event: 'left'});
  });

  socket.on('set-name', (name) => {
    console.log('who login : ' + name);
    socket.username = name;
    // io.emit('users-changed', {user: name, event: 'joined'});
  });

  socket.on('send-message', (message) => {
    io.emit('message', {msg: message.text, user: socket.username, createdAt: new Date()});
  });

  socket.on('liking', (image) => {
    io.emit('liking-notification', { userName: image.userName, otherUserName: image.imageUserName, imageName: image.imageName, createdAt: image.createdAt, whatKindOfNtf: "like"});
  });

  socket.on('applying', (image) => {
    io.emit('applying-notification', { userName: image.userName, otherUserName: image.imageUserName, imageName: image.imageName, applyInput: image.applyInput, createdAt: image.createdAt, whatKindOfNtf: "apply"});
  });

  socket.on('following', (userInfo) => {
    io.emit('following-notification', { userName: userInfo.userName, otherUserName: userInfo.thisOtherUserName, createdAt: userInfo.createdAt, whatKindOfNtf: "follow"});
  });

});

var port = process.env.PORT || 3001;

server.listen(port, function(){
   console.log('listening in http://localhost:' + port);
});
