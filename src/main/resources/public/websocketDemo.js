var flag = 0;
var pm = "";
// small helper function for selecting element by id
let id = id => document.getElementById(id);

//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/chat");
ws.onmessage = msg => updateChat(msg);
ws.onclose = () => alert("WebSocket connection closed");

// Add event listeners to button and input field
// here, we put the 'id' we set in the html file ("submit"). so we're saying: look at that button that i said has the id "submit"
// in my html file. put an event listener on it. when a user clicks on it, ("click" is built in), call the function
// sendAndClearUser(String username, String password). pass it the text, or 'value' that is in the fields which I gave the ids
// "uSername" and "password" to
id("submit").addEventListener("click", () => sendAndClearUser(id("uSername").value), id("password").value);
// this one says, if someone hits a key inside the text field with the id 'uSername'...
id("uSername").addEventListener("keypress", function (e) {
    // if that key is <enter> (whose id is 13)
    if(e.keyCode === 13) {
        // call the function and pass it the value from the box where the user hit enter, plus the value from the other box
        sendAndClearUser(e.target.value, id("password").value);
    }
});
id("password").addEventListener("keypress", function (e) {
    if(e.keyCode === 13) {
        sendAndClearUser(id("uSername").value, e.target.value);
    }
});

id("send").addEventListener("click", () => sendAndClear(id("message").value));
id("message").addEventListener("keypress", function (e) {
    if (e.keyCode === 13) { // Send message if enter is pressed in input field
        sendAndClear(e.target.value);
    }
});

id("submitREG").addEventListener("click", () => sendAndClearReg(id("usernameREG").value), id("passwordREG").value);
id("passwordREG").addEventListener("keypress", function (e) {
    if (e.keyCode === 13) {
        sendAndClearReg(id("usernameREG").value, e.target.value);
    }
});

function sendAndClearReg(username, password){
    if(username !== "" && password !== ""){
        String(username);
        String(password);
        username = "ID:REG" + username + "ID:PW" + password;
        ws.send(username);
        id("usernameREG").value = "";
        id("passwordREG").value = "";
    }

    if (password === "") {
        id("passwordREG").placeholder = "Enter a password";
    }
    if(username === ""){
        id("usernameREG").placeholder = "Enter a username";
    }
}

function sendAndClearUser(uSername, password) {
    if (uSername !== "" && password !== ""){
        String(uSername);
        String(password);
        uSername = "ID:LOGIN" + uSername + "ID:PW" + password;
        ws.send(uSername);
        id("uSername").value = "";
        id("password").value = "";
    }
    id("uSername").value = "";
    id("password").value = "";
    if (password === "") {
        id("password").placeholder = "Enter a password";
    }
    if(uSername === ""){
        id("uSername").placeholder = "Enter a username";
    }
}

function sendAndClear(message) {
    if (message !== "") {
        if(flag == 1){
            String(message);
            message = "ID:PM" + pm + "ID:MSG" + message;
            ws.send(message);
            id("message").value = "";
            flag = 0;
            pm = "";
        } else {
            ws.send(message);
            id("message").value = "";
        }
    }
}

function updateChat(msg) { // Update chat-panel and list of connected users
    let data = JSON.parse(msg.data);
    //System.out.println("data.userMessage: " + data.userMessage);
    id("chat").insertAdjacentHTML("afterbegin", data.userMessage);
    id("userlist").innerHTML = data.userlist.map(user => "<li role=\"presentation\" class=\"link\" value=\"thing\" onclick=\"button('" + user + "')\"><span>" + user + "</span></li>").join("");
}

function button(newuser) {
    if (pm.equals(newuser)) {
        pm = "";
        flag = 0;
        id("message").value = "";
    } else {
        String(newuser);
        pm = newuser;
        id("message").placeholder = "Sending private message to " + newuser;
        flag = 1;
    }
}