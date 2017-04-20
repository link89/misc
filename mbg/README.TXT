Usage:

mvn \
        -Dmybatis.generator.jdbcURL=jdbc:mysql://127.0.0.1:3306/db \
        -Dmybatis.generator.jdbcUserId=user \
        -Dmybatis.generator.jdbcPassword=password \
        mybatis-generator:generate

notice: mysql-connector-java should use 5.1.x
