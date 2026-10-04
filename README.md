# RestAssured API Basics

A beginner-friendly Java project demonstrating REST API testing using **REST Assured** and **TestNG**. This project tests the [JSONPlaceholder](https://jsonplaceholder.typicode.com/) free fake API for testing and prototyping.

---

## 📋 Project Overview

This project showcases fundamental REST API testing concepts including:
- **GET** requests with path parameters and query parameters
- Response validation (status codes, JSON body extraction)
- TestNG test suite setup with logging
- Dual output logging (console + timestamped log files)

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Programming language |
| **Maven** | 3.x | Build & dependency management |
| **REST Assured** | 5.4.0 | REST API testing library |
| **TestNG** | 7.9.0 | Testing framework |
| **Jackson Databind** | 2.17.0 | JSON AST parsing & schema analysis |
| **H2 Database** | 2.2.224 | JDBC database testing & schema discovery |

---

## 📁 Project Structure

```
RestAssured-API-Basics/
├── pom.xml                                   # Maven configuration
├── Jenkinsfile                               # Jenkins CI/CD pipeline
├── README.md                                 # Project documentation
├── src/
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── neel/
│       │           ├── api/
│       │           │   ├── BasicApiTest.java              # Baseline API tests
│       │           │   └── TestExecutionLogger.java       # Console & file logger
│       │           └── selfhealing/
│       │               ├── api/                           # API Self-Healing Layer
│       │               │   ├── ApiHealingEngine.java      # Field, path & request healing
│       │               │   ├── EndpointHealer.java        # Route & version healer
│       │               │   ├── SchemaHealer.java          # Schema diff & diagnostics
│       │               │   ├── SelfHealingJsonPath.java   # Smart JsonPath wrapper
│       │               │   └── SelfHealingResponse.java   # Response wrapper
│       │               ├── db/                            # Database Self-Healing Layer
│       │               │   ├── DatabaseHealingEngine.java # Column, table & SQL healer
│       │               │   ├── DatabaseMetadataModel.java # Schema metadata model
│       │               │   ├── DatabaseSafetyGuard.java   # Strict read-only enforcement
│       │               │   ├── DatabaseSchemaDiscovery.java # JDBC metadata discovery
│       │               │   ├── SelfHealingDatabaseQuery.java # Safe query executor
│       │               │   └── SelfHealingResultSet.java  # Smart ResultSet wrapper
│       │               ├── confidence/                    # Multi-factor Confidence Engine
│       │               │   ├── ConfidenceEngine.java      # Syntactic, semantic & type scoring
│       │               │   ├── ConfidenceLevel.java       # HIGH, MEDIUM, LOW categories
│       │               │   └── ConfidenceScore.java       # Detailed score breakdown
│       │               ├── config/
│       │               │   └── SelfHealingConfig.java     # Priority config loader
│       │               ├── history/
│       │               │   ├── HealingHistoryManager.java # History file persistence
│       │               │   └── HealingRecord.java         # History record model
│       │               ├── report/
│       │               │   ├── FailureCategory.java       # Failure taxonomy enum
│       │               │   ├── FailureClassifier.java     # Automated failure classifier
│       │               │   ├── SelfHealingEvent.java      # Healing attempt event
│       │               │   ├── SelfHealingReport.java     # Aggregated statistics
│       │               │   └── SelfHealingReporter.java   # HTML, JSON & console reports
│       │               ├── security/
│       │               │   └── SecurityMasker.java        # Secret & credential sanitizer
│       │               ├── engine/
│       │               │   └── SelfHealingEngine.java     # Central unified facade
│       │               ├── listener/
│       │               │   └── SelfHealingTestNGListener.java # Suite lifecycle hooks
│       │               └── tests/                         # Verification Suites
│       │                   ├── ApiSelfHealingTest.java    # 10 API verification tests
│       │                   └── DatabaseSelfHealingTest.java # 10 DB verification tests
│       └── resources/
│           └── self-healing.properties               # Self-healing configuration
├── reports/                                           # Generated Self-Healing Reports
│   ├── self-healing-report.html                      # Interactive HTML dashboard
│   ├── self-healing-report.json                      # Machine-readable report
│   └── healing_history.json                          # Persistent healing history
├── logs/                                             # Auto-generated execution logs
└── target/                                           # Maven build output
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher installed
- **Maven 3.6+** installed
- Internet connection (tests call external API)

### Installation

```bash
# Clone or navigate to the project directory
cd RestAssured-API-Basics

# Install dependencies and compile
mvn clean compile test-compile
```

---

## 🧪 Running Tests

### Run All Tests (Baseline API + API Self-Healing + DB Self-Healing)

```bash
mvn test
```

### Run Baseline API Tests Only

```bash
mvn test -Dtest=BasicApiTest
```

### Run API Self-Healing Verification Suite

```bash
mvn test -Dtest=ApiSelfHealingTest
```

### Run Database Self-Healing Verification Suite

```bash
mvn test -Dtest=DatabaseSelfHealingTest
```

### Run with Verbose Output

```bash
mvn test -X
```

---

## 📝 Test Suites

### 1. Baseline API Tests (`BasicApiTest`)

| Test Method | Description | API Endpoint |
|-------------|-------------|--------------|
| `testGetUserById()` | GET single user by ID | `GET /users/1` |
| `testGetAllUsers()` | GET all users | `GET /users` |
| `testGetUserWithPathParam()` | GET user using path parameter | `GET /users/{id}` |
| `testGetUsersWithQueryParam()` | GET users with query filter | `GET /users?username=Bret` |
| `testPostNewUser()` | POST create new user | `POST /users` |
| `testPutUpdateUser()` | PUT update existing user | `PUT /users/1` |
| `testDeleteUser()` | DELETE remove user | `DELETE /users/1` |

### 2. API Self-Healing Verification Suite (`ApiSelfHealingTest`)

1. `test1_OriginalFieldExists`: Normal field extraction without healing.
2. `test2_FieldRenamed`: Auto-heals renamed response field (`userName` → `username`).
3. `test3_NestedFieldChanged`: Auto-heals nested field paths (`user.address.zip` → `user.address.zipcode`).
4. `test4_JsonPathChanged`: Auto-heals modified JSONPath structures (`$.data.user.name` → `$.result.user.name`).
5. `test5_SchemaChanged`: Analyzes contract diffs and outputs mapping diagnostics (`userId` → `id`, `userName` → `username`).
6. `test6_MultiplePossibleMappings`: Disambiguates candidate fields by semantic & type context.
7. `test7_HighConfidenceMapping`: Auto-heals candidate with high confidence ($\ge 90\%$).
8. `test8_LowConfidenceMapping`: Safely rejects unrelated tokens below threshold without masking errors.
9. `test9_EndpointVersionChange`: Safely upgrades API endpoint version (`/api/v1/users` → `/api/v2/users`).
10. `test10_HealingDisabled`: Confirms zero healing when disabled in configuration.

### 3. Database Self-Healing Verification Suite (`DatabaseSelfHealingTest`)

1. `test1_OriginalColumnExists`: Standard JDBC column retrieval without healing.
2. `test2_ColumnRenamed`: Auto-heals renamed column (`customer_name` → `customerName`).
3. `test3_TableRenamed`: Auto-heals renamed table (`customers` → `customer`).
4. `test4_DataTypeChanged`: Evaluates strict type compatibility (`INT` ↔ `BIGINT` vs `INT` ↔ `VARCHAR`).
5. `test5_MultiplePossibleColumns`: Disambiguates table-prefixed columns in relational context.
6. `test6_HighConfidenceMapping`: Auto-heals column mapping with high confidence ($\ge 90\%$).
7. `test7_LowConfidenceMapping`: Safely rejects low-confidence column mappings.
8. `test8_RelationshipValidation`: Prohibits cross-entity mapping (`customer_id` is never mapped to `product_id`).
9. `test9_QueryHealing`: Safely heals and executes query `SELECT customer_name FROM customers WHERE id = 1` as `SELECT CUSTOMERNAME FROM CUSTOMER WHERE id = 1`.
10. `test10_HealingDisabled`: Confirms zero column healing when disabled in configuration.

---

## 📊 Logging & Reports

### Console Output
Tests print formatted execution logs, real-time healing events, and a final summary:
- Status codes & response bodies
- `[API-SELF-HEALING]` candidate diagnostics
- `[DB-SELF-HEALING]` schema & query resolution details
- Final aggregated **Self-Healing Summary**

### Self-Healing Reports (`reports/`)
- **Interactive HTML Dashboard**: `reports/self-healing-report.html`
- **Machine-Readable JSON Report**: `reports/self-healing-report.json`
- **Persistent Healing History**: `reports/healing_history.json`

### Log Files (`logs/`)
Timestamped execution logs capturing both terminal output and API payloads:
```
logs/test-execution-yyyy-MM-dd_HH-mm-ss.txt
```

### TestNG Reports (`target/surefire-reports/`)
- `emailable-report.html` (Summary report)
- `index.html` (Detailed TestNG report)
- `junitreports/` (JUnit XML format for CI/CD)

---

## 🔧 Configuration

### Change Java Version
Edit `pom.xml`:
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

### Change API Base URL
Modify `BasicApiTest.java`:
```java
.baseUri("https://jsonplaceholder.typicode.com")
```

### Add More Dependencies
Add to `<dependencies>` in `pom.xml`:
```xml
<dependency>
    <groupId>group-id</groupId>
    <artifactId>artifact-id</artifactId>
    <version>x.y.z</version>
    <scope>test</scope>
</dependency>
```

---

---

## 🛡️ Production Self-Healing Automation System

This project features an enterprise-grade **Self-Healing Automation System** for both **API Automation** and **Database Testing**.

### 🌟 Key Self-Healing Capabilities

1. **API Response Field & JSONPath Healing**:
   - Automatically recovers from renamed fields (e.g. `userName` → `username`).
   - Adapts to structural shifts in JSONPath (e.g. `$.data.user.name` → `$.result.user.name`).
   - Deep nested object and array navigation.
2. **API Schema & Contract Diffing**:
   - Detects structural diffs, type alignments, and diagnostic mapping proposals without blindly weakening validation.
3. **API Endpoint & Version Healing**:
   - Detects version bumps (e.g. `/api/v1/users` → `/api/v2/users`) from registered known routes.
   - Zero brute-forcing; safe failure if candidate cannot be identified with high confidence.
4. **Database Schema Discovery & Column/Table Healing**:
   - Dynamically discovers tables, columns, data types, primary keys, and foreign keys using JDBC `DatabaseMetaData`.
   - Heals renamed columns (e.g. `customer_name` → `customerName`) and tables (e.g. `customers` → `customer`).
   - Heals SQL queries safely without modifying test or SQL files on disk.
5. **Relationship-Aware & Data Integrity Protection**:
   - Cross-entity mapping prevention (e.g. `customer_id` is never mapped to `product_id`).
   - Data validation integrity: business value mismatches (e.g. `order_status = 'DELIVERED'` vs `'CANCELLED'`) are NEVER healed and strictly FAIL.
6. **Multi-Factor Confidence Engine**:
   - Syntactic similarity (Jaro-Winkler + Levenshtein + Token Jaccard).
   - Domain semantic synonyms and casing normalization.
   - Strict data type compatibility validation.
   - Default confidence threshold: `90%` (`0.90`).
7. **Persistent History & Reporting**:
   - Persistent mappings stored in `reports/healing_history.json`.
   - Comprehensive interactive HTML report (`reports/self-healing-report.html`).
   - Machine-readable JSON report (`reports/self-healing-report.json`).
   - Security masking: credentials, passwords, tokens, and secrets are strictly masked.
   - Safety: database execution is strictly **READ-ONLY** by default.

### ⚙️ Self-Healing Configuration

Managed via `src/test/resources/self-healing.properties`, environment variables, or JVM arguments:

```properties
SELF_HEALING_ENABLED=true
SELF_HEALING_MAX_ATTEMPTS=3
SELF_HEALING_CONFIDENCE_THRESHOLD=0.90
SELF_HEALING_SAVE_HISTORY=true
SELF_HEALING_LOG_LEVEL=INFO
API_SELF_HEALING_ENABLED=true
DB_SELF_HEALING_ENABLED=true
SELF_HEALING_HISTORY_FILE=reports/healing_history.json
SELF_HEALING_REPORT_DIR=reports
```

---

## 📚 Learning Resources

- [REST Assured Documentation](https://rest-assured.io/)
- [TestNG Documentation](https://testng.org/doc/)
- [JSONPlaceholder API](https://jsonplaceholder.typicode.com/)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

---

## 📄 License

This project is for educational purposes. Feel free to use and modify.

---

## 👨💻 Author

**Neel** - API Testing Enthusiast

---

*Happy Testing! 🎉*