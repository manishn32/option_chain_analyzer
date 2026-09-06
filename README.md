# option_chain_analyzer
Helps in fetching NSE option chains data and store it into clickHouse.

## Market projection module

This project now includes a multi-timeframe projection engine with:

- bull/bear scores
- support and resistance detection with strength
- smart-money flow
- strike migration tracking
- breakout probability
- projection score and market regime
- projection accuracy tracking

### SQL setup

Run `src/main/resources/sql/market_projection_tables.sql` in ClickHouse.

### API endpoints

- `POST /api/projections/calculate`
- `GET /api/projections/latest`
- `GET /api/projections/dashboard`
- `POST /api/projections/accuracy`
- `GET /api/projections/accuracy/all`

### Quick try

```powershell
cd C:\Users\SESA796586\Downloads\analyser\optionsAnalyser
.\mvnw.cmd -Dtest=ProjectionCalculatorTest test
```

