[![banner](https://raw.githubusercontent.com/oceanprotocol/art/master/github/repo-banner%402x.png)](https://oceanprotocol.com)

# squid-java

> 🦑 Java client library for Ocean Protocol
> [oceanprotocol.com](https://oceanprotocol.com)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a2285f0f3a664cf091c8e00a243898ce)](https://app.codacy.com/app/ocean-protocol/squid-java?utm_source=github.com&utm_medium=referral&utm_content=oceanprotocol/squid-java&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.com/oceanprotocol/squid-java.svg?branch=develop)](https://travis-ci.com/oceanprotocol/squid-java)
[![Javadocs](http://javadoc.io/badge/com.oceanprotocol/squid.svg)](http://javadoc.io/doc/com.oceanprotocol/squid)

---

## Table of Contents

* [Features](#features)
* [Installation](#installation)
* [Configuration](#configuration)
  * [Using Squid-Java with Barge](#using-squid-java-with-barge)
  * [Dealing with Flowables](#dealing-with-flowables)
* [Documentation](#documentation)
* [Testing](#testing)
  * [Unit Tests](#unit-tests)
  * [Integration Tests](#integration-tests)
  * [Code Coverage](#code-coverage)
* [New Release](#new-release)
* [License](#license)

---

## Features

This library enables to integrate the Ocean Protocol capabilities from JVM clients.

Currently squid-java implements the last version of the [squid-spec (v0.2)](https://github.com/oceanprotocol/dev-ocean/blob/master/doc/architecture/squid.md).

## Installation

Typically in Maven you can add squid-java as a dependency:

```xml
<dependency>
  <groupId>com.oceanprotocol</groupId>
  <artifactId>squid-java</artifactId>
  <version>0.4.0</version>
</dependency>
```

## Configuration

You can configure the library using TypeSafe Config or a Java Properties Object

In case you want to use TypeSafe Config you would need an application.conf file with this shape:

```
keeper.url="http://localhost:8545"
keeper.gasLimit=4712388
keeper.gasPrice=100000000000

aquarius.url="http://localhost:5000"

secretstore.url="http://localhost:12001"

# Contracts addresses
contract.SignCondition.address="0xEEE56e2a630DD29F9A628d618E58bb173911F393"
contract.HashLockCondition.address="0x85cCa2B01adddCA8Df221e6027EE0D7716224202"
contract.LockRewardCondition.address="0x3a3926f3f88F1eE05164404f93FDb3887cbE8e35"
contract.AccessSecretStoreCondition.address="0x19513460bc16254c74AE806683E906478A42B543"
contract.EscrowReward.address="0x8F006DbB3727d18f032C5618595ecDD2EDE13b61"
contract.EscrowAccessSecretStoreTemplate.address="0xD306b5edCDC7819E1EB80B43De6548931706A3f4"
contract.OceanToken.address="0x726baA2f854A3BEC2378a707AeB38c9d933Ebad6"
contract.Dispenser.address="0xF152cF3c67dFD41a317eAe8fAc0e1e8E98724A13"
contract.DIDRegistry.address="0xc354ba9AD5dF1023C2640b14A09E61a500F21546"
contract.ConditionStoreManager.address="0x336EFb3c9E56F713dFdA4CDB3Dd0882F3226b6eE"
contract.TemplateStoreManager.address="0xfeA10BBb093d7fcb1EDf575Aa7e28d37b9DcFcE9"
contract.AgreementStoreManager.address="0x645439117eB378a6d35148452E287a038666Ed67"

consume.basePath = "/tmp"

## Main account
account.main.address="0xaabbcc"
account.main.password="pass"
account.main.credentialsFile="/accounts/parity/aabbcc.json.testaccount"
```

And you can instantiate the API with the following lines:

```java
 Config config = ConfigFactory.load();
 OceanAPI oceanAPI = OceanAPI.getInstance(config);
```

Remember that TypeSafe Config allows you to overwrite the values using environment variables or arguments passed to the JVM.

If you want to use Java's Properties, you just need to create a Properties object with the same properties of the application.conf. You can read these Properties from a properties file, or define the values of these properties in your code:

```java
// Default values for KEEPER_URL, KEEPER_GAS_LIMIT, KEEPER_GAS_PRICE, AQUARIUS_URL, SECRETSTORE_URL, CONSUME_BASE_PATH
Properties properties = new Properties();
properties.put(OceanConfig.MAIN_ACCOUNT_ADDRESS, "0xaabbcc");
properties.put(OceanConfig.MAIN_ACCOUNT_PASSWORD,"pass");
properties.put(OceanConfig.MAIN_ACCOUNT_CREDENTIALS_FILE,"/accounts/parity/aabbcc.json.testaccount");
properties.put(OceanConfig.DID_REGISTRY_ADDRESS, "0x01daE123504DDf108E0C65a42190516E5c5dfc07");
properties.put(OceanConfig.SERVICE_EXECUTION_AGREEMENT_ADDRESS, "0x21668cE2116Dbc48AC116F31678CfaaeF911F7aA");
properties.put(OceanConfig.PAYMENT_CONDITIONS_ADDRESS, "0x38A531cc85A58adCb01D6a249E33c27CE277a2D1");
properties.put(OceanConfig.ACCESS_CONDITIONS_ADDRESS, "0x605FAF898Fc7c2Aa847Ba0D558b5251c0F128Fd7");
properties.put(OceanConfig.TOKEN_ADDRESS, "0xe749e2f8482810b11b838ae8c5eb69e54d610411");
properties.put(OceanConfig.OCEAN_MARKET_ADDRESS, "0xf9e633cbeeb2a474d3fe22261046c99e805beec4");

OceanAPI oceanAPIFromProperties = OceanAPI.getInstance(properties);
```

Once you have initialized the API you can call the methods through their corresponding API class. For instance:

```java
 Balance balance = oceanAPI.getAccountsAPI().balance(oceanAPI.getMainAccount());

 String filesJson = metadataBase.toJson(metadataBase.base.files);
 String did = DID.builder().getHash();
 String encryptedDocument = oceanAPI.getSecretStoreAPI().encrypt(did, filesJson, 0);

 Flowable<OrderResult> response = oceanAPI.getAssetsAPI().order(did, SERVICE_DEFINITION_ID);
 boolean result = oceanAPI.getAssetsAPI().consume(orderResult.getServiceAgreementId(), did, SERVICE_DEFINITION_ID, "/tmp");
```

### Using Squid-Java with Barge

If you are using [Barge](https://github.com/oceanprotocol/barge/) for playing with the Ocean Protocol stack, you can use the following command to run the components necessary to have a fully functional environment:

`KEEPER_VERSION=v0.8.5 bash start_ocean.sh --latest --no-pleuston --local-spree-node`

After a few minutes, when Keeper has deployed the contracts, the ABI files describing the Smart Contracts can be found 
in the `${HOME}/.ocean/keeper-contracts/artifacts/` folder. Depending on the network you are using, each ABI includes the 
address where the Smart Contract is deployed in each network.

If you want to run the integration tests on your local machine, you can execute the Bash script `src/test/resources/scripts/updateConfAddresses.sh`
to update the addresses to use in your `src/test/resources/application.conf` file.

### Dealing with Flowables

Squid-java uses web3j to interact with Solidity's Smart Contracts. It relies on [RxJava](https://github.com/ReactiveX/RxJava) to deal with asynchronous calls.

The order method in AssetsAPI returns a Flowable over an OrderResult object. It's your choice if you want to handle this in a synchronous or asynchronous fashion.

If you prefer to deal with this method in a synchronous way, you will need to block the current thread until you get a response:

```java
 Flowable<OrderResult> response = oceanAPI.getAssetsAPI().order(did, SERVICE_DEFINITION_ID);
 OrderResult orderResult = response.blockingFirst();
```

On the contrary, if you want to handle the response asynchronously, you will need to subscribe to the Flowable:

```java
response.subscribe(
     orderResultEvent -> {
         if (orderResultEvent.isAccessGranted())
             System.out.println("Access Granted for Service Agreement " + orderResultEvent.getServiceAgreementId());
         else if (orderResultEvent.isPaymentRefund())
             System.out.println("There was a problem with Service Agreement " + orderResultEvent.getServiceAgreementId() + " .Payment Refund");
     }
 );
```

The subscribe method will launch a new Thread to react to the events of the Flowable.
More information: [RxJava](https://github.com/ReactiveX/RxJava/wiki) , [Flowable](http://reactivex.io/RxJava/2.x/javadoc/)

## Documentation

All the API documentation is hosted of javadoc.io:

- **[https://www.javadoc.io/doc/com.oceanprotocol/squid](https://www.javadoc.io/doc/com.oceanprotocol/squid)**

You can also generate the Javadoc locally using the following command:

```bash
mvn javadoc:javadoc
```

## Testing

You can run both, the unit and integration tests by using:

```bash
mvn clean verify -P all-test
```

### Unit Tests

You can execute the unit tests only using the following command:

```bash
mvn clean test
```

### Integration Tests

The execution of the integration tests require to have running the complete Ocean stack using [Ocean Barge](https://github.com/oceanprotocol/barge).

After having `barge` in your environment, you can run the components needed running:

```bash
KEEPER_VERSION=v0.8.0 bash start_ocean.sh --latest --no-pleuston --local-spree-node --force-pull
```

If you have older versions of the docker images is recommended to delete all them to be sure you are running the last version of the stack.

You can execute the integration tests using the following command:

```bash
mvn clean verify -P integration-test
```

### Code Coverage

The code coverage reports are generated using the JaCoCo Maven plugin. Reports are generated in the `target/site` folder.

## New Release

The `bumpversion.sh` script helps to bump the project version. You can execute the script using as first argument {major|minor|patch} to bump accordingly the version.

## License

```
Copyright 2018 Ocean Protocol Foundation Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

