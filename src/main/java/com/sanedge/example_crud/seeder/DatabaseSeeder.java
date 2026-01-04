package com.sanedge.example_crud.seeder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.example_crud.domain.requests.role.CreateRoleRequest;
import com.sanedge.example_crud.domain.requests.role.FindAllRoles;
import com.sanedge.example_crud.domain.requests.user.CreateUserRequest;
import com.sanedge.example_crud.domain.requests.user.FindAllUsers;
import com.sanedge.example_crud.model.Role;
import com.sanedge.example_crud.model.User;
import com.sanedge.example_crud.model.UserRole;
import com.sanedge.example_crud.repository.RoleRepository;
import com.sanedge.example_crud.repository.UserRepository;
import com.sanedge.example_crud.repository.UserRoleRepository;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;

public class DatabaseSeeder {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;

  public DatabaseSeeder(Pool pool) {
    this.userRepository = new UserRepository(pool);
    this.roleRepository = new RoleRepository(pool);
    this.userRoleRepository = new UserRoleRepository(pool);
  }

  public Future<Void> seed() {
    logger.info("🌱 Starting database seeding...");

    FindAllRoles roleReq = new FindAllRoles();
    roleReq.setPage(1);
    roleReq.setPageSize(10);

    return roleRepository.getRoles(roleReq)
        .compose(existingRoles -> {
          if (!existingRoles.getData().isEmpty()) {
            logger.info("📊 Database already contains data, skipping seeding");
            return Future.succeededFuture();
          }

          return seedRoles()
              .compose(v -> {
                logger.info("✅ Roles seeded successfully");
                return seedUsers();
              })
              .compose(v -> {
                logger.info("✅ Users seeded successfully");
                return seedUserRoles();
              })
              .onSuccess(v -> logger.info("✅ Database seeding completed successfully"))
              .onFailure(err -> logger.error("❌ Database seeding failed: {}", err.getMessage(), err));
        });
  }

  private Future<Void> seedRoles() {
    List<CreateRoleRequest> roles = List.of(
        CreateRoleRequest.builder().name("ADMIN").build(),
        CreateRoleRequest.builder().name("USER").build(),
        CreateRoleRequest.builder().name("MANAGER").build());

    logger.info("🔧 Seeding {} roles...", roles.size());

    return createRole(roles.get(0))
        .compose(v -> createRole(roles.get(1)))
        .compose(v -> createRole(roles.get(2)))
        .compose(v -> {
          logger.info("✅ All {} roles created", roles.size());
          return Future.succeededFuture();
        });
  }

  private Future<Void> createRole(CreateRoleRequest role) {
    return roleRepository.createRole(role)
        .onSuccess(r -> logger.info("✓ Created role: {} (ID: {})", r.getRoleName(), r.getRoleId()))
        .onFailure(err -> logger.error("✗ Failed to create role {}: {}", role.getName(), err.getMessage()))
        .mapEmpty();
  }

  private Future<Void> seedUsers() {
    List<CreateUserRequest> users = List.of(
        CreateUserRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("admin@example.com")
            .password(hashPassword("admin123"))
            .build(),
        CreateUserRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("user@example.com")
            .password(hashPassword("user123"))
            .build(),
        CreateUserRequest.builder()
            .firstName("Bob")
            .lastName("Johnson")
            .email("manager@example.com")
            .password(hashPassword("manager123"))
            .build());

    logger.info("👥 Seeding {} users...", users.size());

    return createUser(users.get(0))
        .compose(v -> createUser(users.get(1)))
        .compose(v -> createUser(users.get(2)))
        .compose(v -> {
          logger.info("✅ All {} users created", users.size());
          return Future.succeededFuture();
        });
  }

  private Future<Void> createUser(CreateUserRequest user) {
    return userRepository.createUser(user)
        .onSuccess(u -> logger.info("✓ Created user: {} {} - {} (ID: {})",
            u.getFirstname(), u.getLastname(), u.getEmail(), u.getUserId()))
        .onFailure(err -> {
          logger.error("✗ Failed to create user {} {}: {}",
              user.getFirstName(), user.getLastName(), err.getMessage());
          logger.error("Full error: ", err);
        })
        .mapEmpty();
  }

  private Future<Void> seedUserRoles() {
    logger.info("🔗 Starting user-role assignment seeding...");

    FindAllUsers userReq = new FindAllUsers();
    userReq.setPage(1);
    userReq.setPageSize(10);

    FindAllRoles roleReq = new FindAllRoles();
    roleReq.setPage(1);
    roleReq.setPageSize(10);

    return userRepository.getUsers(userReq)
        .compose(usersResp -> {
          logger.info("📊 Found {} users in database:", usersResp.getData().size());
          usersResp.getData().forEach(u -> logger.info("  - User ID: {}, Name: {} {}, Email: {}",
              u.getUserId(), u.getFirstname(), u.getLastname(), u.getEmail()));

          if (usersResp.getData().size() < 3) {
            String msg = String.format("Not enough users to seed user roles. Expected 3, found %d",
                usersResp.getData().size());
            logger.error(msg);
            return Future.failedFuture(msg);
          }

          return roleRepository.getRoles(roleReq)
              .compose(rolesResp -> {
                logger.info("📊 Found {} roles in database:", rolesResp.getData().size());
                rolesResp.getData()
                    .forEach(r -> logger.info("  - Role ID: {}, Name: {}", r.getRoleId(), r.getRoleName()));

                if (rolesResp.getData().size() < 3) {
                  String msg = String.format("Not enough roles to seed user roles. Expected 3, found %d",
                      rolesResp.getData().size());
                  logger.error(msg);
                  return Future.failedFuture(msg);
                }

                logger.info("🔗 Assigning roles to users...");

                List<User> users = usersResp.getData();
                List<Role> roles = rolesResp.getData();

                return createUserRoleAssignment(users.get(0), roles.get(0))
                    .onSuccess(v -> logger.info("✓ Assigned {} to {}",
                        roles.get(0).getRoleName(), users.get(0).getEmail()))
                    .compose(v -> createUserRoleAssignment(users.get(1), roles.get(1)))
                    .onSuccess(v -> logger.info("✓ Assigned {} to {}",
                        roles.get(1).getRoleName(), users.get(1).getEmail()))
                    .compose(v -> createUserRoleAssignment(users.get(2), roles.get(2)))
                    .onSuccess(v -> logger.info("✓ Assigned {} to {}",
                        roles.get(2).getRoleName(), users.get(2).getEmail()))
                    .compose(v -> {
                      logger.info("✅ Successfully seeded all user-role assignments");
                      return Future.succeededFuture();
                    })
                    .mapEmpty();
              });
        });
  }

  private Future<UserRole> createUserRoleAssignment(User user, Role role) {
    UserRole userRole = UserRole.builder()
        .userId(user.getUserId())
        .roleId(role.getRoleId())
        .createdAt(Timestamp.from(Instant.now()))
        .updatedAt(Timestamp.from(Instant.now()))
        .build();

    return userRoleRepository.assignRoleToUser(userRole)
        .onFailure(err -> logger.error("Failed to assign role {} to user {}: {}",
            role.getRoleName(), user.getEmail(), err.getMessage()));
  }

  private String hashPassword(String plainPassword) {
    return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
  }

  public static void runSeeder(
      Pool pool,
      boolean enableSeeder) {

    if (!enableSeeder) {
      logger.info("🚫 Database seeder disabled, skipping");
      return;
    }

    DatabaseSeeder seeder = new DatabaseSeeder(pool);

    seeder.seed()
        .onSuccess(v -> logger.info("🎉 Database seeder executed successfully"))
        .onFailure(err -> logger.error("💥 Database seeder failed: {}", err.getMessage(), err));
  }
}
