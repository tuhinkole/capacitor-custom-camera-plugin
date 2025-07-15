import AVFoundation
import Capacitor

@objc(CustomCameraPlugin)
public class CustomCameraPlugin: CAPPlugin {
    private let implementation = CustomCamera()

    @objc func startCamera(_ call: CAPPluginCall) {
        implementation.startCamera()
        call.resolve()
    }

    @objc func startRecording(_ call: CAPPluginCall) {
        implementation.startRecording()
        call.resolve()
    }

    @objc func takePictureWithEffect(_ call: CAPPluginCall) {
        implementation.takePictureWithEffect()
        call.resolve()
    }

    @objc func stopRecording(_ call: CAPPluginCall) {
        let videoPath = implementation.stopRecording()
        call.resolve(["videoPath": videoPath])
    }
}