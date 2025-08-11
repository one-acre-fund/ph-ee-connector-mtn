# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.3] - 2025-08-11
### Changed
- [CP-3784](https://oneacrefund.atlassian.net/browse/CP-3784) Modify the namespace for payment endpoint response to match the request namespace.
- 
## [1.1.2] - 2025-08-07
### Changed
- [CP-3774](https://oneacrefund.atlassian.net/browse/CP-3774) Move oafReference inside extension in MTN payment payload

## [1.1.1] - 2025-08-05
### Fixed
- [CP-3769](https://oneacrefund.atlassian.net/browse/CP-3769) Extract the Correct Account Number & MSISDN from MTN Paybill Payload

## [1.1.0] - 2025-05-16
### Added
- [CP-3553](https://oneacrefund.atlassian.net/browse/CP-3553) Implement MTN Paybill flow

## [1.0.5] - 2024-11-22
### Updated
- [SER-3220](https://oneacrefund.atlassian.net/browse/SER-3220) Update actuator endpoints

## [1.0.4] - 2023-11-14
### Updated
- [SER-2099](https://oneacrefund.atlassian.net/browse/SER-2099) Add the OAF account number on the MTN portal reports (updated to send the account number under payeeNote too)

## [1.0.3] - 2023-11-10
### Changed
-  Remove the phone number + prefix


## [1.0.2] - 2023-11-07
### Added
- [SER-2099](https://oneacrefund.atlassian.net/browse/SER-2099) Add the OAF account number on the MTN portal reports

## [1.0.1] - 2023-09-12
### Fixed
- [SER-1995](https://oneacrefund.atlassian.net/browse/SER-1995) HandLe cases where the financialTransactionId is not received from the payment provider
