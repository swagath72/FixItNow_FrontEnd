import sys
import os

# Add backend to path to use its models
backend_path = r"c:\xampp\htdocs\fixitnow-backend"
if backend_path not in sys.path:
    sys.path.append(backend_path)

try:
    import database
    import models
    from sqlalchemy.orm import Session
    
    db = database.SessionLocal()
    # Query for users with 'admin' role (case-insensitive)
    admins = db.query(models.User).filter(models.User.role.ilike('%admin%')).all()
    if not admins:
        print("No admin users found.")
    for u in admins:
        # Note: We can't see the plain text password if it was hashed, 
        # but in this project it seems they might be stored plainly or we can find the test ones.
        print(f"ID={u.id}, Email='{u.email}', Role='{u.role}', Password='{u.password}'")
    db.close()
except Exception as e:
    print(f"Error: {e}")
