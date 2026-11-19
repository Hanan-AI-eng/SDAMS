package model;

import util.FileHandler;
import util.Validator;

/**
 * STAND-IN for S1's abstract User class. Swap in S1's version when ready; the
 * Administrator module only relies on the constructor, getters and toFileLine().
 *
 * users.txt: userId | name | username | password | role | phone | email
 */
public abstract class User extends Record {

    private String name;
    private String username;
    private String password;
    private final Role role;
    private String phone;
    private String email;

    protected User(String id, String name, String username, String password,
                   Role role, String phone, String email) {
        super(id);
        if (role == null || !id.startsWith(role.getIdPrefix())) {
            throw new IllegalArgumentException("ID " + id + " does not match role " + role);
        }
        this.role = role;
        setName(name);
        setUsername(username);
        setPassword(password);
        setPhone(phone);
        setEmail(email);
    }

    /** Each role opens its own dashboard (polymorphism used by the login screen). */
    public abstract void openDashboard();

    @Override
    public String toFileLine() {
        return FileHandler.join(getId(), name, username, password, role.name(), phone, email);
    }

    // ---- getters ----
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    // ---- validated setters (encapsulation) ----
    public final void setName(String name) {
        this.name = Validator.requireText(name, "Name", 50);
    }

    public final void setUsername(String username) {
        if (!Validator.isValidUsername(username)) {
            throw new IllegalArgumentException(
                    "Username must be 4-20 letters, digits or underscores.");
        }
        this.username = username.trim();
    }

    public final void setPassword(String password) {
        if (Validator.hasPipe(password)) {
            throw new IllegalArgumentException("Password must not contain the | character.");
        }
        if (!Validator.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least "
                    + Validator.MIN_PASSWORD_LENGTH + " characters with no spaces.");
        }
        this.password = password;
    }

    public final void setPhone(String phone) {
        if (!Validator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone must be 10-11 digits starting with 0 (e.g. 0123456789).");
        }
        this.phone = phone.trim();
    }

    public final void setEmail(String email) {
        if (Validator.hasPipe(email) || !Validator.isValidEmail(email)) {
            throw new IllegalArgumentException("Email is not valid (e.g. name@sda.com).");
        }
        this.email = email.trim();
    }
}
