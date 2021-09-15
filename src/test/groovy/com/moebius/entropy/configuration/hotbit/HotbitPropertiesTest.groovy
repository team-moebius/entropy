package com.moebius.entropy.configuration.hotbit

import com.moebius.entropy.configuration.HotbitProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(
        properties = [
                "exchange.hotbit.websocket.uri=1234",
                "exchange.hotbit.websocket.timeout=1234",
                "exchange.hotbit.rest.scheme=scheme",
                "exchange.hotbit.rest.host=host",
                "exchange.hotbit.rest.openOrders=openOrders",
                "exchange.hotbit.rest.requestOrders=requestOrders",
                "exchange.hotbit.rest.orderBook=orderBook",
                "exchange.hotbit.rest.cancelOrders=cancelOrders",
        ],
        classes = [ConfigurationPropertiesTestConfig, HotbitProperties])
class HotbitPropertiesTest extends Specification {
    @Autowired
    private HotbitProperties configuration

    def loadTest(){
        expect:
        configuration != null
        configuration.getWebSocket().tap {
            assert uri == "1234"
            assert timeout == 1234
        }
        configuration.getRest().tap {
            assert scheme == "scheme"
            assert host == "host"
            assert openOrders == "openOrders"
            assert requestOrders == "requestOrders"
            assert orderBook == "orderBook"
            assert cancelOrders == "cancelOrders"
        }
    }
}
