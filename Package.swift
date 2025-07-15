// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorCustomCamera",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorCustomCamera",
            targets: ["CustomCameraPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "CustomCameraPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CustomCameraPlugin"),
        .testTarget(
            name: "CustomCameraPluginTests",
            dependencies: ["CustomCameraPlugin"],
            path: "ios/Tests/CustomCameraPluginTests")
    ]
)