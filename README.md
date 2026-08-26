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

---

## 📁 Project Structure

```
RestAssured-API-Basics/
├── pom.xml                          # Maven configuration
├── src/
│   └── test/
│       └── java/
│           └── com/
│               └── neel/
│                   └── api/
│                       ├── BasicApiTest.java      # Main test class
│                       └── TestExecutionLogger.java  # Custom logging utility
├── logs/                              # Auto-generated test execution logs
└── target/                            # Maven build output
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

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=BasicApiTest
```

### Run with Verbose Output

```bash
mvn test -X
```

---

## 📝 Test Cases

| Test Method | Description | API Endpoint |
|-------------|-------------|--------------|
| `testGetUserById()` | GET single user by ID | `GET /users/1` |
| `testGetAllUsers()` | GET all users | `GET /users` |
| `testGetUserWithPathParam()` | GET user using path parameter | `GET /users/{id}` |
| `testGetUsersWithQueryParam()` | GET users with query filter | `GET /users?username=Bret` |

### Test Features Demonstrated

- ✅ **Base URI configuration**
- ✅ **Path parameters** (`/users/{id}`)
- ✅ **Query parameters** (`?username=Bret`)
- ✅ **Status code validation** (200 OK)
- ✅ **JSON response extraction** using JsonPath
- ✅ **Assertions** (equals, notNull, true)
- ✅ **Console output** with formatted results

---

## 📊 Logging & Reports

### Console Output
Tests print formatted output to console showing:
- Status codes
- Extracted JSON fields
- Full response bodies
- Test pass/fail status

### Log Files
Each test run creates a timestamped log file in `logs/`:
```
logs/test-execution-2026-08-26_22-06-04.txt
```

### TestNG Reports
After running tests, view HTML reports in:
```
target/surefire-reports/
├── emailable-report.html    # Summary report
├── index.html               # Detailed report
└── junitreports/            # JUnit XML format
```

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