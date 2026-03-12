package com.example.loanoriginationsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
        "DB_URL=jdbc:h2:mem:prod_profile_smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "DB_USERNAME=sa",
        "DB_PASSWORD=password",
        "DB_DRIVER=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
    }
)
@ActiveProfiles("prod")
class ProdProfileContextSmokeTest {

    @Test
    void contextLoadsWithProdProfile() {
    }
}
