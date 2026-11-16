# Ranges MTT — referência do app

Todos os ranges são gerados a partir de **tabelas de percentual por posição × faixa de stack** (em `Ranges.kt`), aplicados sobre uma ordem de força das 169 mãos. Dois ordenamentos: **DEEP** (abrir/pagar com stack fundo: valoriza suited, conectadas e pares pequenos) e **PUSH** (all-ins: equity/Nash).

Faixas de stack (BB): **80-100+**, **50-79**, **30-49**, **20-29**, **15**, **10**. Em 15 e 10 BB a abertura é **shove** (push/fold).

## 1) Abertura (RFI) por posição

### 80-100+ BB
| Posição | % | Range |
|---|---|---|
| UTG | 13.0% | Pares: 66+ ; Suited: A5s-A3s, A9s+, KTs+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KQo |
| UTG+1 | 14.5% | Pares: 66+ ; Suited: A5s-A3s, A8s+, KTs+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KJo+ |
| MP | 17.0% | Pares: 66+ ; Suited: A5s-A2s, A8s+, K9s+, Q9s+, J9s+, T9s, 98s, 87s ; Offsuit: ATo+, KJo+, QJo |
| HJ | 21.0% | Pares: 55+ ; Suited: A2s+, K9s+, Q9s+, J9s+, T8s+, 98s, 87s ; Offsuit: ATo+, KTo+, QTo+, JTo |
| CO | 28.0% | Pares: 33+ ; Suited: A2s+, K5s+, Q8s+, J8s+, T8s+, 97s+, 86s+, 75s+, 65s, 54s ; Offsuit: A8o+, KTo+, QTo+, JTo, T9o |
| BTN | 46.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q2s+, J2s+, T6s+, 95s+, 84s+, 74s+, 63s+, 52s+, 43s ; Offsuit: A2o+, K8o+, Q9o+, J9o+, T9o |
| SB | 42.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q4s+, J6s+, T6s+, 96s+, 85s+, 74s+, 63s+, 53s+, 43s ; Offsuit: A2o+, K9o+, Q9o+, J9o+, T9o |

### 50-79 BB
| Posição | % | Range |
|---|---|---|
| UTG | 13.0% | Pares: 66+ ; Suited: A5s-A3s, A9s+, KTs+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KQo |
| UTG+1 | 14.5% | Pares: 66+ ; Suited: A5s-A3s, A8s+, KTs+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KJo+ |
| MP | 17.0% | Pares: 66+ ; Suited: A5s-A2s, A8s+, K9s+, Q9s+, J9s+, T9s, 98s, 87s ; Offsuit: ATo+, KJo+, QJo |
| HJ | 21.0% | Pares: 55+ ; Suited: A2s+, K9s+, Q9s+, J9s+, T8s+, 98s, 87s ; Offsuit: ATo+, KTo+, QTo+, JTo |
| CO | 28.0% | Pares: 33+ ; Suited: A2s+, K5s+, Q8s+, J8s+, T8s+, 97s+, 86s+, 75s+, 65s, 54s ; Offsuit: A8o+, KTo+, QTo+, JTo, T9o |
| BTN | 45.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q2s+, J4s+, T6s+, 95s+, 85s+, 74s+, 63s+, 52s+, 43s ; Offsuit: A2o+, K8o+, Q9o+, J9o+, T9o |
| SB | 42.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q4s+, J6s+, T6s+, 96s+, 85s+, 74s+, 63s+, 53s+, 43s ; Offsuit: A2o+, K9o+, Q9o+, J9o+, T9o |

### 30-49 BB
| Posição | % | Range |
|---|---|---|
| UTG | 13.0% | Pares: 66+ ; Suited: A5s-A3s, A9s+, KTs+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KQo |
| UTG+1 | 14.5% | Pares: 66+ ; Suited: A5s-A3s, A8s+, KTs+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KJo+ |
| MP | 17.0% | Pares: 66+ ; Suited: A5s-A2s, A8s+, K9s+, Q9s+, J9s+, T9s, 98s, 87s ; Offsuit: ATo+, KJo+, QJo |
| HJ | 21.0% | Pares: 55+ ; Suited: A2s+, K9s+, Q9s+, J9s+, T8s+, 98s, 87s ; Offsuit: ATo+, KTo+, QTo+, JTo |
| CO | 27.0% | Pares: 33+ ; Suited: A2s+, K5s+, Q8s+, J8s+, T8s+, 97s+, 86s+, 76s, 65s, 54s ; Offsuit: A8o+, KTo+, QTo+, JTo |
| BTN | 42.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q4s+, J6s+, T6s+, 96s+, 85s+, 74s+, 63s+, 53s+, 43s ; Offsuit: A2o+, K9o+, Q9o+, J9o+, T9o |
| SB | 40.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q5s+, J6s+, T7s+, 96s+, 85s+, 74s+, 63s+, 53s+, 43s ; Offsuit: A4o+, K9o+, Q9o+, J9o+, T9o |

### 20-29 BB
| Posição | % | Range |
|---|---|---|
| UTG | 12.0% | Pares: 66+ ; Suited: A5s-A4s, A9s+, KTs+, QTs+, JTs, T9s ; Offsuit: AJo+, KQo |
| UTG+1 | 13.5% | Pares: 66+ ; Suited: A5s-A3s, A9s+, KTs+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KQo |
| MP | 16.0% | Pares: 66+ ; Suited: A5s-A2s, A8s+, K9s+, QTs+, JTs, T9s, 98s ; Offsuit: ATo+, KJo+, QJo |
| HJ | 20.0% | Pares: 55+ ; Suited: A2s+, K9s+, Q9s+, J9s+, T9s, 98s, 87s ; Offsuit: ATo+, KTo+, QTo+ |
| CO | 25.0% | Pares: 44+ ; Suited: A2s+, K7s+, Q8s+, J8s+, T8s+, 97s+, 87s, 76s, 65s, 54s ; Offsuit: A9o+, KTo+, QTo+, JTo |
| BTN | 38.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q5s+, J6s+, T7s+, 96s+, 85s+, 74s+, 64s+, 53s+, 43s ; Offsuit: A4o+, K9o+, QTo+, JTo, T9o |
| SB | 38.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q5s+, J6s+, T7s+, 96s+, 85s+, 74s+, 64s+, 53s+, 43s ; Offsuit: A4o+, K9o+, QTo+, JTo, T9o |

### 15 BB (push/fold)
| Posição | % | Range |
|---|---|---|
| UTG | 11.0% | Pares: 88+ ; Suited: A8s+, K9s+, QTs+, JTs, T9s ; Offsuit: AJo+, KQo |
| UTG+1 | 12.0% | Pares: 88+ ; Suited: A8s+, K9s+, Q9s+, J9s+, T9s ; Offsuit: AJo+, KQo |
| MP | 14.0% | Pares: 77+ ; Suited: A5s-A4s, A7s+, K9s+, Q9s+, J9s+, T9s ; Offsuit: AJo+, KJo+ |
| HJ | 18.0% | Pares: 66+ ; Suited: A2s+, K8s+, Q9s+, J9s+, T8s+, 98s ; Offsuit: ATo+, KJo+, QJo |
| CO | 23.0% | Pares: 55+ ; Suited: A2s+, K5s+, Q8s+, J8s+, T8s+, 97s+, 87s ; Offsuit: A9o+, KTo+, QTo+ |
| BTN | 34.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q3s+, J6s+, T7s+, 96s+, 85s+, 75s+, 64s+, 54s ; Offsuit: A8o+, K9o+, QTo+, JTo, T9o |
| SB | 40.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q2s+, J3s+, T6s+, 95s+, 85s+, 74s+, 64s+, 53s+, 43s ; Offsuit: A5o, A7o+, K9o+, Q9o+, J9o+, T9o |

### 10 BB (push/fold)
| Posição | % | Range |
|---|---|---|
| UTG | 20.0% | Pares: 66+ ; Suited: A2s+, K7s+, Q8s+, J8s+, T8s+, 98s, 87s ; Offsuit: ATo+, KTo+, QJo |
| UTG+1 | 22.0% | Pares: 55+ ; Suited: A2s+, K5s+, Q8s+, J8s+, T8s+, 97s+, 87s ; Offsuit: A9o+, KTo+, QJo |
| MP | 25.0% | Pares: 44+ ; Suited: A2s+, K4s+, Q8s+, J8s+, T8s+, 97s+, 87s, 76s ; Offsuit: A9o+, KTo+, QTo+, JTo |
| HJ | 29.0% | Pares: 33+ ; Suited: A2s+, K2s+, Q6s+, J7s+, T7s+, 97s+, 86s+, 76s, 65s, 54s ; Offsuit: A8o+, KTo+, QTo+, JTo |
| CO | 35.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q2s+, J5s+, T6s+, 96s+, 85s+, 75s+, 64s+, 53s+ ; Offsuit: A8o+, K9o+, QTo+, JTo, T9o |
| BTN | 48.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q2s+, J2s+, T3s+, 93s+, 84s+, 74s+, 63s+, 52s+, 43s ; Offsuit: A3o+, K7o+, Q8o+, J9o+, T9o |
| SB | 58.0% | Pares: 22+ ; Suited: A2s+, K2s+, Q2s+, J2s+, T2s+, 92s+, 82s+, 73s+, 62s+, 52s+, 42s+, 32s ; Offsuit: A2o+, K5o+, Q7o+, J8o+, T8o+, 98o, 87o |

## 2) Enfrentando um open (3-bet/reshove e call)

Cenários-chave. Em 20-29, 15 e 10 BB a coluna "3-bet" vira **reshove (all-in)**.

### BTN vs CO
| Stack | 3-bet / shove | Call |
|---|---|---|
| 80-100+ | **7.7%** Pares: TT+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s, 87s, 76s, 65s ; Offsuit: AQo+ | **11.8%** Pares: 99-22 ; Suited: AJs-A8s, KTs+, Q9s+, J9s+ ; Offsuit: AJo-ATo, KJo+, QJo |
| 50-79 | **7.7%** Pares: TT+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s, 87s, 76s, 65s ; Offsuit: AQo+ | **11.2%** Pares: 99-22 ; Suited: AJs-A8s, KTs+, QTs+, JTs ; Offsuit: AJo-ATo, KJo+, QJo |
| 30-49 | **6.5%** Pares: TT+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s, 87s, 76s ; Offsuit: AKo | **8.1%** Pares: 99-44 ; Suited: AJs-A9s, KTs+, QTs+, JTs ; Offsuit: AQo-AJo, KQo |
| 20-29 | **8.4%** Pares: 99+ ; Suited: ATs+, KTs+, QTs+, JTs ; Offsuit: AJo+ | **4.8%** Pares: 88-77 ; Suited: A5s, A9s-A8s, K9s, Q9s, J9s, T9s ; Offsuit: KJo+ |
| 15 | **13.9%** Pares: 77+ ; Suited: A5s-A4s, A7s+, K9s+, Q9s+, J9s+, T9s ; Offsuit: AJo+, KJo+ | — |
| 10 | **22.3%** Pares: 55+ ; Suited: A2s+, K5s+, Q8s+, J8s+, T8s+, 97s+, 87s ; Offsuit: A9o+, KTo+, QJo | — |

### CO vs HJ
| Stack | 3-bet / shove | Call |
|---|---|---|
| 80-100+ | **5.4%** Pares: JJ+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s ; Offsuit: AKo | **9.2%** Pares: TT-22 ; Suited: AJs-ATs, KTs+, QTs+, JTs ; Offsuit: AQo-AJo, KQo |
| 50-79 | **5.4%** Pares: JJ+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s ; Offsuit: AKo | **8.6%** Pares: TT-22 ; Suited: AJs-ATs, KJs+, QJs, JTs ; Offsuit: AQo-AJo, KQo |
| 30-49 | **4.8%** Pares: JJ+ ; Suited: A5s-A2s, AKs, K9s, T9s ; Offsuit: AKo | **6.8%** Pares: TT-44 ; Suited: AQs-ATs, KJs+, QJs ; Offsuit: AQo-AJo |
| 20-29 | **7.1%** Pares: TT+ ; Suited: ATs+, KTs+, QTs+, JTs ; Offsuit: AQo+ | **3.0%** Pares: 99-88 ; Suited: A9s ; Offsuit: AJo, KQo |
| 15 | **11.0%** Pares: 88+ ; Suited: A8s+, K9s+, QTs+, JTs, T9s ; Offsuit: AJo+, KQo | — |
| 10 | **17.0%** Pares: 66+ ; Suited: A2s+, K9s+, Q9s+, J9s+, T9s ; Offsuit: ATo+, KJo+, QJo | — |

### SB vs BTN
| Stack | 3-bet / shove | Call |
|---|---|---|
| 80-100+ | **9.7%** Pares: 99+ ; Suited: A5s-A2s, ATs+, K9s, KJs+, QJs, T9s, 98s, 87s, 76s, 65s ; Offsuit: AQo+ | **6.5%** Pares: 88-66 ; Suited: A9s-A8s, KTs, QTs, JTs ; Offsuit: AJo-ATo, KJo+ |
| 50-79 | **9.7%** Pares: 99+ ; Suited: A5s-A2s, ATs+, K9s, KJs+, QJs, T9s, 98s, 87s, 76s, 65s ; Offsuit: AQo+ | **6.2%** Pares: 88-66 ; Suited: A9s, KTs, QTs, JTs ; Offsuit: AJo-ATo, KJo+ |
| 30-49 | **8.7%** Pares: 99+ ; Suited: A5s-A2s, AJs+, K9s, KQs, T9s, 98s, 87s, 76s, 65s ; Offsuit: AQo+ | **5.0%** Pares: 88-66 ; Suited: ATs, KJs-KTs, QTs+, JTs ; Offsuit: AJo, KQo |
| 20-29 | **10.1%** Pares: 88+ ; Suited: A9s+, KTs+, QTs+, JTs ; Offsuit: AJo+, KQo | **3.5%** Pares: 77 ; Suited: A5s, A8s-A7s, K9s, Q9s, J9s, T9s ; Offsuit: KJo |
| 15 | **17.9%** Pares: 66+ ; Suited: A2s+, K8s+, Q9s+, J9s+, T8s+, 98s ; Offsuit: ATo+, KJo+, QJo | — |
| 10 | **29.9%** Pares: 33+ ; Suited: A2s+, K2s+, Q6s+, J7s+, T7s+, 96s+, 86s+, 75s+, 65s, 54s ; Offsuit: A8o+, KTo+, QTo+, JTo | — |

### BB vs BTN
| Stack | 3-bet / shove | Call |
|---|---|---|
| 80-100+ | **10.6%** Pares: 99+ ; Suited: A5s-A2s, ATs+, K9s, KJs+, QJs, T9s, 98s, 87s, 76s, 65s ; Offsuit: AJo+ | **38.8%** Pares: 88-22 ; Suited: A9s-A6s, K8s-K2s, KTs, QTs-Q2s, J2s+, T8s-T3s, 97s-93s, 86s-84s, 75s-74s, 64s-63s, 52s+, 43s ; Offsuit: ATo-A2o, K7o+, Q8o+, J9o+, T9o |
| 50-79 | **10.6%** Pares: 99+ ; Suited: A5s-A2s, ATs+, K9s, KJs+, QJs, T9s, 98s, 87s, 76s, 65s ; Offsuit: AJo+ | **34.8%** Pares: 88-22 ; Suited: A9s-A6s, K8s-K2s, KTs, QTs-Q2s, J3s+, T8s-T6s, 97s-95s, 86s-85s, 75s-74s, 64s-63s, 52s+, 43s ; Offsuit: ATo-A2o, K8o+, Q9o+, J9o+, T9o |
| 30-49 | **9.7%** Pares: 99+ ; Suited: A5s-A2s, ATs+, K9s, KJs+, QJs, T9s, 98s, 87s, 76s, 65s ; Offsuit: AQo+ | **26.4%** Pares: 88-22 ; Suited: A9s-A6s, K8s-K2s, KTs, QTs-Q5s, J6s+, T8s-T7s, 97s-96s, 86s-85s, 75s-74s, 64s, 53s+, 43s ; Offsuit: A5o, AJo-A7o, K9o+, QTo+, JTo, T9o |
| 20-29 | **12.8%** Pares: 88+ ; Suited: A5s, A8s+, K9s+, Q9s+, J9s+, T9s ; Offsuit: AJo+, KJo+ | **19.0%** Pares: 77-22 ; Suited: A4s-A2s, A7s-A6s, K8s-K2s, Q8s-Q3s, J8s-J6s, T8s-T7s, 96s+, 86s+, 75s+, 64s+, 54s ; Offsuit: ATo-A8o, KTo, QTo+, JTo |
| 15 | **23.2%** Pares: 55+ ; Suited: A2s+, K5s+, Q8s+, J8s+, T8s+, 97s+, 87s ; Offsuit: A9o+, KTo+, QTo+ | **10.7%** Pares: 44-22 ; Suited: K4s-K2s, Q7s-Q3s, J7s-J6s, T7s, 96s, 86s-85s, 75s+, 64s+, 54s ; Offsuit: A8o, K9o, JTo, T9o |
| 10 | **39.1%** Pares: 22+ ; Suited: A2s+, K2s+, Q2s+, J4s+, T6s+, 95s+, 85s+, 74s+, 64s+, 53s+, 43s ; Offsuit: A7o+, K9o+, Q9o+, J9o+, T9o | — |

### BB vs MP
| Stack | 3-bet / shove | Call |
|---|---|---|
| 80-100+ | **6.5%** Pares: TT+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s, 87s, 76s ; Offsuit: AKo | **21.1%** Pares: 99-22 ; Suited: AJs-A6s, K8s-K5s, KTs+, Q8s+, J8s+, T8s, 97s, 86s, 65s, 54s ; Offsuit: AQo-A8o, KTo+, QTo+, JTo |
| 50-79 | **6.5%** Pares: TT+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s, 87s, 76s ; Offsuit: AKo | **19.0%** Pares: 99-22 ; Suited: AJs-A6s, K8s-K7s, KTs+, Q9s+, J8s+, T8s, 97s, 65s, 54s ; Offsuit: AQo-A9o, KTo+, QTo+, JTo |
| 30-49 | **5.4%** Pares: JJ+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s ; Offsuit: AKo | **14.9%** Pares: TT-44 ; Suited: AJs-A6s, KTs+, Q9s+, J9s+, 87s ; Offsuit: AQo-ATo, KTo+, QTo+ |
| 20-29 | **7.5%** Pares: 99+ ; Suited: ATs+, KTs+, QTs+, JTs ; Offsuit: AQo+ | **10.7%** Pares: 88-66 ; Suited: A9s-A2s, K9s-K7s, Q9s, J9s, T8s+, 98s ; Offsuit: AJo-ATo, KJo+, QJo |
| 15 | **11.6%** Pares: 88+ ; Suited: A8s+, K9s+, Q9s+, J9s+, T9s ; Offsuit: AJo+, KQo | **5.7%** Pares: 77-66 ; Suited: A7s-A2s, K8s ; Offsuit: ATo, KJo, QJo |
| 10 | **18.3%** Pares: 66+ ; Suited: A2s+, K7s+, Q9s+, J9s+, T8s+, 98s ; Offsuit: ATo+, KJo+, QJo | — |

### BB vs UTG
| Stack | 3-bet / shove | Call |
|---|---|---|
| 80-100+ | **5.4%** Pares: JJ+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s ; Offsuit: AKo | **19.5%** Pares: TT-22 ; Suited: AJs-A6s, K8s, KTs+, Q9s+, J8s+, T8s, 97s, 87s, 76s, 65s ; Offsuit: AQo-A9o, KTo+, QTo+, JTo |
| 50-79 | **5.4%** Pares: JJ+ ; Suited: A5s-A2s, AQs+, K9s, T9s, 98s ; Offsuit: AKo | **17.3%** Pares: TT-22 ; Suited: AJs-A6s, K8s, KTs+, Q9s+, J9s+, T8s, 87s ; Offsuit: AQo-ATo, KTo+, QTo+, JTo |
| 30-49 | **4.8%** Pares: JJ+ ; Suited: A5s-A2s, AKs, K9s, T9s ; Offsuit: AKo | **13.1%** Pares: TT-44 ; Suited: AQs-A8s, KTs+, Q9s+, J9s+, 98s, 87s ; Offsuit: AQo-ATo, KJo+, QJo |
| 20-29 | **6.5%** Pares: TT+ ; Suited: ATs+, KTs+, QJs ; Offsuit: AQo+ | **8.9%** Pares: 99-77 ; Suited: A9s-A3s, K9s, QTs-Q9s, J9s+, T9s ; Offsuit: AJo, KJo+, QJo |
| 15 | **9.4%** Pares: 99+ ; Suited: ATs+, KTs+, QTs+, JTs ; Offsuit: AJo+, KQo | **4.8%** Pares: 88-77 ; Suited: A5s-A3s, A9s-A7s, K9s, Q9s, J9s, T9s ; Offsuit: KJo |
| 10 | **14.2%** Pares: 77+ ; Suited: A5s-A3s, A7s+, K9s+, Q9s+, J9s+, T9s ; Offsuit: AJo+, KJo+ | — |

## 3) Ajuste pela fase do torneio e perfil de stack

| Fase | Prêmio de risco (equity extra p/ call) | Multiplicador de abertura |
|---|---|---|
| Início | +0.0% | ×1.0 |
| Meio | +1.0% | ×1.0 |
| Pré-bolha | +3.5% | ×0.95 |
| Bolha | +7.0% | ×0.9 |
| Premiação | +3.0% | ×1.0 |
| Mesa final | +6.0% | ×0.92 |

Perfil de stack (botão **Stack**): *Normal* ×1,0 · *Curto* (prêmio de risco ×1,35) · *Grande* (prêmio ×0,5 e aberturas ×1,15 — chip leader pressiona).

Além disso, nas fases com pressão de ICM os ranges de 3-bet e call são escalados para baixo (`tight3 = 1 − 1,5·ICM`, `tightCall = 1 − 3·ICM`).

**Ante (botão Ante):** com ante ligado, os percentuais de abertura/shove são multiplicados por ×1,12 (os ranges ficam mais largos, como em MTTs com ante). É uma aproximação, não uma tabela própria.

**Pot (botão Pot):** *c/ apostas* = o número lido em "pot" já inclui as apostas da rua; *s/ apostas* = o app mostra só o pot anterior, e o assistente soma as apostas na mesa antes de calcular pot odds.

**Posições:** modelo de 8 lugares (UTG, UTG+1, MP, HJ, CO, BTN, SB, BB). Em mesa de 9, trate UTG+2 como MP.
