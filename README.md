# ⚙️ E-Commerce Platform (Backend API)

A robust RESTful API service built with **Java Spring Boot**, **Spring Security**, **JPA / Hibernate**, and **MySQL**. Provides endpoints for user authentication (JWT), product catalog filtering, user wishlist management, and image file uploads.

🔗 **Frontend Repo**: [ck-ecommerce-frontend](https://github.com/MrChengKang/ck-ecommerce-frontend)

---

## ✨ Key Features
- **Stateless Authentication**: JWT-based security with custom parsing logic (`JwtUtils`) and role-based access control.
- **Relational Database Design**: Structured MySQL schemas mapping Users, Products, and Wishlists using JPA/Hibernate.
- **File Upload Service**: REST endpoint for handling dynamic user profile avatar uploads.
- **CORS Configured**: Fully integrated with Vercel frontend deployment.

---

## 🛠️ Tech Stack
- **Language**: Java 17+
- **Framework**: Spring Boot, Spring Security
- **Database**: MySQL, Hibernate / Spring Data JPA
- **Build Tool**: Maven
- **Deployment**: Render

---

## ⚙️ Getting Started

1. **Clone the repository**:
   ```bash
   git clone [https://github.com/MrChengKang/ck-ecommerce-backend.git](https://github.com/MrChengKang/ck-ecommerce-backend.git)
   cd ck-ecommerce-backend
