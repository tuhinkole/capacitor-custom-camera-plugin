import { CustomCamera } from 'capacitor-custom-camera';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    CustomCamera.echo({ value: inputValue })
}
