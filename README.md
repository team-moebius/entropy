# Entropy

## Definition

<img src="https://github.com/team-moebius/entropy/blob/master/entropy-ui.png?raw=true" width="50%" />

Entropy는 특정 종목의 호가창을 추적하는 복합 주문 시스템으로서, 자동 더미 매매(주문 등록 - 주문 취소) 및 호가창 채우기 / 수동 현재가 매매를 지원합니다.

## Features

* ### Automatic (자동 매도/매수 설정)
  
  1. #### 매도/매수 단위 호가 수 설정 (ex : 6)

     매수 및 매도 호가창에 채울 단위 호가수를 의미합니다. 

     <img src="https://github.com/team-moebius/entropy/blob/master/sample-order-book.png?raw=true" width="50%" />

     먼저 단위 호가 수에 대해 정의하면, 거래소에서 지원하는(보여지는) 호가창이 위 스크린샷과 같다면 단위 호가 수는 6이 됩니다. 단위 호가 수 같은 경우, 보통 거래소에서 지원하는 최대 단위 호가 주문 수보다 같거나 많게 설정해야 합니다. 즉 위 경우는 6보다 크거나 같은 값이 됩니다.

     *Entropy*는 기본적으로 **호가창 채우기**를 지원하는데, 위의 예시를 기준으로 단위 호가 수를 6개로 설정하고, 현재가 11.35$에서 매수가 더 발생해 11.36$가 현재가가 되었다고 가정해봅시다. 그러면 매도 호가창에는 최대 11.41$까지만 보이고 있다가 11.42$가 보이기 시작할 것입니다.

     이 경우, 11.42$에 주문이 없다면 매도 호가창에는 5개의 호가(11.41$ ~ 11.37$)가 보이고, 매수 호가창에는 6개의 호가(11.36$ ~ 11.31$)가 보일 것입니다. 이럴 경우 **기존 가장 낮은 가격(11.30$)에 걸려있던 매수 주문들을 모두 취소하고, 11.42$에 랜덤한 수량의 매도 주문을 넣어줘야 합니다**.
     보통 시장 상황에 따라 단위 호가 수를 6개보다 더 많은 수를 넣어 미리 채워놓는 형태가 되기 때문에 호가창 채우기는 일정량 버퍼를 두고 진행하게 됩니다.

  

  2. #### 매도/매수 주문 수량 범위 설정 (ex : 100.00 ~ 1500.99)

     단위 호가마다 주문을 넣을 수량의 범위를 의미합니다.
     예를 들어 100.00 ~ 1500.99 라고 지정하면, 각 호가마다 최소 100.00, 최대 1500.99의 수량 중 임의의 수량을 잡아서 주문을 요청합니다. 위 스크린샷을 기준으로 한다면 매도 주문을 11.36$ - 150.34, 11.37$ - 385.27, 11.38$ - 100.5 ... 이런식으로 등록하게 됩니다. 매수 주문도 이와 같습니다.

  

  3. #### 매도/매수 분할주문 & 재주문 설정 (ex : 2 ~ 7 주문 / 1.00초 / 1 ~ 2회)

     ii.에서 정한 주문 수량 범위 중 임의로 결정된 수량을 기준으로 잡고, 임의로 분할 한 뒤에 주문 등록 - 취소 - 등록...을 반복합니다. 

     가장 처음에 나오는 **주문 수 범위**는 얼마나 분할을 할 건지 나타내는 값으로, **최소 2분할 ~ 최대 10분할**까지 지원하며 몇 분할을 할 건지는 매번 바뀌는 임의의 값입니다.
     두번째 나오는 **시간**은 주문 주기(period)를 의미하고, 마지막에 나오는 **횟수 범위**는 주문 주기 동안 주문 등록 - 주문 취소를 몇 회를 할 것인지 의미합니다. 주문 횟수는 **0회 이상**를 지원하며, 몇 회 주문 등록 - 취소를 할 지는 마찬가지로 매번 바뀌는 임의의 값입니다.

     시나리오를 들어서 다시 한번 설명해보겠습니다.

     * **시나리오 1.** (2 ~ 7 / 1.5초 / 0 ~ 3회 (주문 수량 : 1500))

       1. 2 ~ 7 중 임의의 값으로 5가 잡혔고, 주문 수량 1500을 5분할로 임의로 나눠서 물량을 배정합니다.
          여기서는 계산하기 편하게 100 / 200 / 300 / 400 / 500 로 잡겠습니다.

          > 절대 균등 분할하지 않습니다.

          

       2. 시간은 1.5초, 횟수는 0 ~ 3회 중 2회가 잡혔습니다. 이렇게 될 경우 1.5초동안 주문 수량 1500(100 / 200 / 300 / 400 / 500)을 주문 등록 - 취소 - 등록 - 취소(등록 - 취소 2회)하여 주문을 5건 등록 - 5건 취소 - 5건 등록 - 5건 취소하게 됩니다.

     * **시나리오 2.** (2 ~ 7 / 1.5초 / 0 ~ 3회 (주문 수량 : 1000))

       1. 2 ~ 7 중 임의의 값 2가 잡혔고, 이번에 주문 수량은 1000이 잡혀서 임의의 2분할로 나눠서 물량을 배정합니다.
          이번에는 300 / 700으로 잡겠습니다.
       2. 시간은 마찬가지로 1.5초, 횟수는 0 ~ 3회중 0회가 잡혔습니다. 0회일 경우 주문을 하지 않습니다.

     

  4. #### 반복 시장가 매도/매수 설정 (ex : 1.00 ~ 2.00 / 1.00초 / 1 ~ 2회)

     수량 범위 중 임의의 값을 매매 수량으로 정한뒤, n초 동안 횟수 범위 중 임의의 값을 매매 횟수로 정해 시장가 매도/매수하는 기능입니다. 제목에 적힌 예시 값으로 설명하면 1.00 ~ 2.00 사이의 랜덤한 수량을 1초 동안 1 ~ 2 사이의 랜덤한 횟수(**최소 1회 이상**)로 시장가 매도/매수 한다는 의미입니다. 시나리오로 설명하면 다음과 같습니다.

     * **시나리오** (1.00 ~ 5.00 / 2.5초 / 1 ~ 3회)
       1. 1.00 ~ 5.00 중 주문 수량 3.5가 잡혔습니다.
       2. 1 ~ 3회중 2회가 잡혔습니다.
       3. 2.5초 동안 주문 수량 3.5를 2회 매수/매도 합니다. 총 주문 거래량은 7(3.5 * 2)이 됩니다.

     

  5. #### 주문 최적화

     주문 최적화는 시스템 UI상 존재하지 않는 기능입니다. 이 시스템은 사람이 직접 같은 거래소 계정에서 수동 매매를 진행하는 경우도 커버해야 되기 때문에, 시스템을 가동할 때 가장 처음 **호가창 채우기**를 진행하면서 기존에 등록해둔 주문들을 **점진적**으로 취소하고 호가창을 채워야 합니다. 이를 주문 최적화라고 합니다.

     예를 들어 10$, 11$, 12$, 13$, 14$에 수동 주문이 등록되어 있고, 12$가 거래가일 경우 10$를 취소하고 시스템이 관리하는 자동 주문으로 10$를 등록하고, 11$를 취소하고 마찬가지로 11$를 등록하는 형태로 진행하게 됩니다.

  

* ### Manual (수동 매매 설정)

  1. #### 매도/매수 현재가 주문 설정 (ex : 1100 ~ 2000)

     이 설정은 실제 시세를 움직이는 용도로 사용합니다. 관리자가 시스템에 접속을 한 뒤에 수량 설정을 하고, 수동으로 현재가 매매를 요청하는 기능입니다. 상세한 내용은 아래 시나리오로 설명하겠습니다.

     * **시나리오** (호가창이 10$(현재가) / 1000, 9$ / 500인 상태의 종목)
       1. 매도 주문 수량 1100 ~ 2000으로 설정하고, 임의의 수량 1500이 잡혔습니다.
       2. 임의 분할을 진행합니다. 3분할로 잡혀서 300, 500, 700으로 나눠졌습니다.
       3. 현재가인 10$에 3분할로 나눈 매도 주문을 등록합니다.
       4. 10$에 걸려있는 체결되지 않은 매도 수량 500을 취소합니다. 참고로 현재가는 9$로 내려가게 됩니다.
       5. 매수도 위 시나리오 같은 방식으로 진행되며, 체결되지 않은 물량은 바로 취소합니다.

     

## Architecture

<img src="https://github.com/team-moebius/entropy/blob/master/entropy-structure.png?raw=true" />
