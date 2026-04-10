import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description 'returns current account for authenticated user'
    request {
        method GET()
        url '/api/accounts/me'
        headers {
            header 'Authorization': 'Bearer token'
        }
    }
    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body(
                login: 'demo',
                name: 'Иванов Иван',
                birthdate: '2001-01-01',
                sum: 100
        )
    }
}
