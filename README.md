<br/>
<p align="center">
  <h3 align="center">eDastakhat</h3>

  <p align="center">
    A Java-based digital signer tool for securing PFX files and signing PDF documents. Supports custom signature options and timestamping for enhanced document validation.
    <br/>
    <br/>
  </p>
</p>

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Usage](#usage)
    - [Basic Command](#basic-command)
    - [Action Types](#action-types)
    - [Examples](#examples)
    - [Payload Explanations](#payload-explanations)
      - [PFX Process Payload](#pfx-process-payload)
      - [PDF Signing Process Payload](#pdf-signing-process-payload)

## Introduction
I have created this Java-based digital signer tool to sign PDF documents using a PFX file.

## Features
- Converting PFX into JKS for PDF signing.
- Signing PDF files.
- Controlling signature placeholders.
- Embedding signature timestamps.
- LTV (Long Term Validation) support (coming soon).

## Prerequisites
- Java `javaRuntime-8` (*32-bit*)
- A valid digital signature certificate in `PFX` format issued by a valid CA.
- Application `eDastakhat` JAR file [Download from the Release section](https://github.com/code-muni/eDastakhat/releases)

## Usage

### Basic Command to Sign PDF
```bash
java -jar /path/to/EDastakhatApplication.jar <ACTION_TYPE> <JSON_PAYLOAD_FILE_PATH> [RESULT_SAVE_DIR_PATH]
```
#### Explanation of Command Arguments
- `<ACTION_TYPE>`: Specifies the action to be performed by the application. Possible allowed values are:
  - `-PFX` or `-P`: Process the PFX file to secure it.
  - `-SIGNATURE` or `-S`: Sign a PDF file.
  - `-HELP` or `-H`: Display help messages and provide examples of payloads files.
- `<JSON_PAYLOAD_FILE_PATH>`: The path to the JSON file containing the payload with details about the PFX file or the PDF signing process. This JSON file must be correctly formatted according to the type of action being performed.
- `[RESULT_SAVE_DIR_PATH]` (Optional): The directory path where the result will be saved. If this argument is not provided, the result will be saved in the same directory as the JSON payload file.
- `v` or `-V` or `-version`: Displays the current version of the application and exits.

> **IMPORTANT NOTE**: By providing these arguments correctly, the application will execute the desired action, process the provided payload, and save the results as specified.

### Action Types
- `-PFX` or `-P`: Process PFX file and secure PFX.
- `-SIGNATURE` or `-S`: Sign a PDF file.
- `-HELP` or `-H`: Display help messages and provide payload examples.

### Examples
- **To display the version of the application:**
```bash
    java -jar /path/to/EDastakhatApplication.jar -v
- ```

- **To process a PFX file:**
```bash
java -jar /path/to/EDastakhatApplication.jar -P /path/of/PFX-Payload.json
```

- **To sign a PDF file:**
```bash
java -jar /path/to/EDastakhatApplication.jar -S /path/of/PDF-Payload.json 
```
 
> **IMPORTANT NOTE**: Example `payload` files can be found in the same directory as this application when executing Help commands `-H`.

---

### Payload explanations
#### PFX Process Payload
To process a PFX file, the JSON payload should include the following fields:
```json
{
  "pfx": "PFX base64 encoded content",
  "password": "password123"
}
```

- `pfx`: This field should contain the _base64 encoded content_ of your PFX file. The PFX file is a PKCS#12 archive file format
- `password`: This is the password for the PFX file. It is used to access the cryptographic content within the PFX file

Ensure your JSON payload file is correctly formatted and includes both fields for the application to process the PFX file successfully.

#### PDF Signing Process Payload
To sign a PDF file, the JSON payload should include the following fields:
```json
{
  "certInfo": {
    "pfxPath": "/path/to/certificate.pfx",
    "password": "password123"
  },
  "options": {
    "page": "L",
    "coord": [0, 0, 0, 0],
    "reason": "Signing document",
    "location": "New York",
    "customText": "Approved by John Doe",
    "greenTick": true,
    "changesAllowed": false,
    "timestamp": {
      "enabled": true,
      "url": "https://timestamp.server.com",
      "username": "user123",
      "password": "pass123"
    },
    "enableLtv": true
  },
  "pdf": {
    "base64Content": "base64EncodedContent",
    "password": "pdfPassword"
  }
}
```

- `certInfo`: Contains information about the PFX certificate.
  - `pfxPath`: Path to the PFX file.
  - `password`: Password for the PFX file.
- `options`: Various options for signing the PDF.
  - `page`: Specifies the page for the signature. Supported values are:
    - `"L"`: Last page.
    - `"A"`: All pages.
    - `"F"`: First page.
    - Any numeric value as a string (e.g., "3" or "4"): Specifies a particular page number in the PDF.
  - `coord`: Coordinates for the signature placement on the page in the format [x, y, width, height].
  - `reason`: Reason for signing the document. (this field is optional you can leave blank string)
  - `location`: Location where the document is being signed. (this field is optional you can leave blank string)
  - `customText`: Custom text to include in the signature. (this field is optional you can leave blank string)
  - `greenTick`: Whether to include a green tick in the signature. (Datatype: boolean)
  - `changesAllowed`: Whether changes are allowed after signing. (Datatype: boolean)
  - `timestamp`: Information about the timestamping service. 
    - `enabled`: Whether timestamping is enabled. (Datatype: boolean)
    - `url`: URL of the timestamping server. (this field is optional you can leave blank string)
    - `username`: Username for the timestamping server. (only require if url is protected)
    - `password`: Password for the timestamping server. (Only require if url is protected)
  - `enableLtv`: Whether to enable Long Term Validation (LTV).(Datatype: boolean)
- `pdf`: Contains the PDF file information.
  - `base64Content`: Base64 encoded content of the PDF file.
  - `password`: Password for the PDF file (if it is password protected).

Ensure your JSON payload file is correctly formatted and includes all necessary fields for the application to sign the PDF file successfully.