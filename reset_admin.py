import sys
import os

# Use the backend's internal database session to reset the admin
backend_path = r"c:\xampp\htdocs\fixitnow-backend"
if backend_path not in sys.path:
    sys.path.append(backend_path)

try:
    import database
    import models
    from sqlalchemy.orm import Session
    
    db = database.SessionLocal()
    admin_email = "admin@fixitnow.com"
    admin = db.query(models.User).filter(models.User.email == admin_email).first()
    
    if admin:
        print(f"Updating existing admin: {admin_email}")
        admin.password = "admin123" # Set as plain text for the current login logic
        admin.role = "admin"
    else:
        print(f"Creating new admin: {admin_email}")
        admin = models.User(
            email=admin_email,
            password="admin123",
            role="admin",
            full_name="System Admin",
            phone="0000000000"
        )
        db.add(admin)
    
    db.commit()
    print("Admin password has been reset to 'admin123' (plain text).")
    db.close()
except Exception as e:
    print(f"Error: {e}")
