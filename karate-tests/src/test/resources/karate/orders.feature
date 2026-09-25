Feature: Order placement API

  Background:
    * url baseUrl
    * def uniqueEmail = 'karate-' + java.lang.System.currentTimeMillis() + '@example.com'

    Given path '/api/auth/register'
    And request { name: 'Karate Tester', email: '#(uniqueEmail)', password: 'password123' }
    When method post
    Then status 201

    Given path '/api/auth/login'
    And request { email: '#(uniqueEmail)', password: 'password123' }
    When method post
    Then status 200
    * def authHeader = 'Bearer ' + response.token

  Scenario: place an order with the default test card and see it in order history

    Given path '/api/products'
    When method get
    Then status 200
    * def product = response[0]

    Given path '/api/orders'
    And header Authorization = authHeader
    And request { items: [{ productId: '#(product.id)', quantity: 1 }] }
    When method post
    Then status 201
    And match response.status == 'PLACED'
    And match response.total == product.price
    And match response.items[0].productId == product.id
    And match response.items[0].quantity == 1
    * def orderId = response.id

    Given path '/api/orders/my'
    And header Authorization = authHeader
    When method get
    Then status 200
    And match response[*].id contains orderId

  Scenario: placing an order without a token is rejected

    Given path '/api/orders'
    And request { items: [{ productId: 1, quantity: 1 }] }
    When method post
    Then status 401

  Scenario: placing an order for a non-existent product returns 404

    Given path '/api/orders'
    And header Authorization = authHeader
    And request { items: [{ productId: 999999, quantity: 1 }] }
    When method post
    Then status 404

  Scenario: ordering more than the available stock returns 409 and rejects the order

    Given path '/api/products'
    When method get
    Then status 200
    * def product = response[0]

    Given path '/api/orders'
    And header Authorization = authHeader
    And request { items: [{ productId: '#(product.id)', quantity: '#(product.stock + 1)' }] }
    When method post
    Then status 409
    And match response.message contains 'Insufficient stock'

  Scenario: a declined card returns 402 and leaves stock unchanged

    Given path '/api/products'
    When method get
    Then status 200
    * def product = response[0]
    * def stockBefore = product.stock

    Given path '/api/orders'
    And header Authorization = authHeader
    And request { items: [{ productId: '#(product.id)', quantity: 1 }], cardNumber: '4000000000000002' }
    When method post
    Then status 402
    And match response.message == 'Payment was declined for this card'

    Given path '/api/products/' + product.id
    When method get
    Then status 200
    And match response.stock == stockBefore
