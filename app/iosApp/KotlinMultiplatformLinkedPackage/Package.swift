// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(path: "subpackages/io_github_mirzemehdi_kmpauth_firebase_3_0_4"),
    .package(path: "subpackages/io_github_mirzemehdi_kmpauth_google_3_0_4"),
    .package(path: "subpackages/dev_gitlive_firebase_auth_3_0_0_alpha01"),
    .package(path: "subpackages/dev_gitlive_firebase_firestore_3_0_0_alpha01"),
    .package(path: "subpackages/dev_gitlive_firebase_messaging_3_0_0_alpha01"),
    .package(path: "subpackages/dev_gitlive_firebase_functions_3_0_0_alpha01"),
    .package(path: "subpackages/dev_gitlive_firebase_analytics_3_0_0_alpha01"),
    .package(path: "subpackages/dev_gitlive_firebase_app_3_0_0_alpha01")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "io_github_mirzemehdi_kmpauth_firebase_3_0_4", package: "io_github_mirzemehdi_kmpauth_firebase_3_0_4"),
        .product(name: "io_github_mirzemehdi_kmpauth_google_3_0_4", package: "io_github_mirzemehdi_kmpauth_google_3_0_4"),
        .product(name: "dev_gitlive_firebase_auth_3_0_0_alpha01", package: "dev_gitlive_firebase_auth_3_0_0_alpha01"),
        .product(name: "dev_gitlive_firebase_firestore_3_0_0_alpha01", package: "dev_gitlive_firebase_firestore_3_0_0_alpha01"),
        .product(name: "dev_gitlive_firebase_messaging_3_0_0_alpha01", package: "dev_gitlive_firebase_messaging_3_0_0_alpha01"),
        .product(name: "dev_gitlive_firebase_functions_3_0_0_alpha01", package: "dev_gitlive_firebase_functions_3_0_0_alpha01"),
        .product(name: "dev_gitlive_firebase_analytics_3_0_0_alpha01", package: "dev_gitlive_firebase_analytics_3_0_0_alpha01"),
        .product(name: "dev_gitlive_firebase_app_3_0_0_alpha01", package: "dev_gitlive_firebase_app_3_0_0_alpha01")
      ]
    )
  ]
)
