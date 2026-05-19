<div align="center">
  <h1>🌊 Jal Drishti</h1>
  <p><b>A Comprehensive Groundwater Management & Agricultural Advisory System</b></p>
  
  [![React](https://img.shields.io/badge/React-18-blue?style=for-the-badge&logo=react)](https://reactjs.org/)
  [![FastAPI](https://img.shields.io/badge/FastAPI-API-009688?style=for-the-badge&logo=fastapi)](https://fastapi.tiangolo.com/)
  [![Supabase](https://img.shields.io/badge/Supabase-PostgreSQL-3ECF8E?style=for-the-badge&logo=supabase)](https://supabase.com/)
  [![Groq](https://img.shields.io/badge/Groq-Llama_3.3_70B-f55036?style=for-the-badge&logo=groq)](https://groq.com/)
  [![Vite](https://img.shields.io/badge/Vite-Build_Tool-646CFF?style=for-the-badge&logo=vite)](https://vitejs.dev/)
</div>

<br />

<div align="center">
  <em>Empowering Indian farmers with technology-driven insights for sustainable groundwater management and agricultural prosperity.</em>
</div>

<hr />

## 🌟 Overview

**Jal Drishti** is a sophisticated, full-stack application designed to track groundwater levels, assess drought risks, and forecast water trends for villages across Maharashtra. Featuring a robust backend powered by **FastAPI** and **Supabase**, and an elegant frontend built with **React** and **TailwindCSS**, it brings critical agricultural data directly to farmers.

At the heart of the system is the **Jal Drishti AI Chatbot**, powered by **Groq** and the **Llama 3.3 70B** model. This AI acts as a virtual agricultural mentor, providing context-aware, village-specific advice in English, Hindi, and Marathi.

<br />

## 🏗️ Technology Stack

<table align="center" width="100%">
  <tr>
    <td align="center" width="33%">
      <h3>Frontend</h3>
      <img src="https://skillicons.dev/icons?i=react,ts,vite,tailwind" />
      <br/><br/>
      <b>React 18</b> (Hooks & Context)<br/>
      <b>TypeScript</b> (Type Safety)<br/>
      <b>Vite</b> (Fast Bundler)<br/>
      <b>TailwindCSS</b> (Styling)<br/>
      <b>Recharts / Chart.js</b> (Data Viz)
    </td>
    <td align="center" width="33%">
      <h3>Backend</h3>
      <img src="https://skillicons.dev/icons?i=fastapi,python,supabase" />
      <br/><br/>
      <b>FastAPI</b> (Async API framework)<br/>
      <b>Python 3.10+</b><br/>
      <b>Pydantic</b> (Data validation)<br/>
      <b>HTTPX</b> (Async HTTP requests)<br/>
      <b>Uvicorn</b> (ASGI Server)
    </td>
    <td align="center" width="33%">
      <h3>AI & Database</h3>
      <br/>
      <img src="https://img.shields.io/badge/Groq-f55036?style=flat-square&logo=groq&logoColor=white" height="32" />
      <br/><br/>
      <b>Groq Cloud</b> (Inference Engine)<br/>
      <b>Llama 3.3 70B Versatile</b> (AI Model)<br/>
      <b>Supabase</b> (PostgreSQL)<br/>
      <b>Row Level Security</b><br/>
      <b>Real-time Subscriptions</b>
    </td>
  </tr>
</table>

<br />

## ✨ Core Features

| Feature | Description |
| :--- | :--- |
| **🌊 Groundwater Monitoring** | Real-time tracking of water levels across 1000+ villages. |
| **🔮 Trend Forecasting** | Advanced predictive analytics projecting water depth for upcoming years. |
| **🔍 Risk Analysis** | Automated drought risk assessment categorizing villages from SAFE to HIGH risk. |
| **🤖 AI Chatbot (Groq)** | Context-aware agricultural advisory using Llama 3.3 70B via Groq Cloud. |
| **🌍 Multilingual Support** | Full seamless support for English, Hindi, and Marathi (i18n). |
| **📊 Interactive Dashboards** | Dynamic charts visualizing historical groundwater trends vs predictions. |

<br />

## 🗄️ Database Architecture

The system utilizes a structured **Supabase (PostgreSQL)** database. The primary schema includes:

```sql
-- 1. Groundwater Master Data (Historical & Current)
CREATE TABLE groundwater_cleaned_final (
    id SERIAL PRIMARY KEY,
    village VARCHAR(255) NOT NULL,
    block VARCHAR(255) NOT NULL,
    district VARCHAR(255) NOT NULL,
    y2014_jan DECIMAL(8,2), -- Historical depth data across years and months
    y2014_may DECIMAL(8,2),
    -- ... (Additional year/month columns up to current year)
    created_at TIMESTAMP DEFAULT NOW()
);

-- 2. Groundwater Predictions
CREATE TABLE groundwater_predictions (
    id SERIAL PRIMARY KEY,
    village VARCHAR(255) NOT NULL,
    district VARCHAR(255) NOT NULL,
    block VARCHAR(255) NOT NULL,
    season VARCHAR(50) NOT NULL, -- e.g., Pre-monsoon, Post-monsoon
    predicted_2024 DECIMAL(8,2),
    predicted_2025 DECIMAL(8,2),
    confidence_low DECIMAL(8,2),
    confidence_high DECIMAL(8,2),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 3. Village Risk Assessment
CREATE TABLE groundwater_village_risk (
    id SERIAL PRIMARY KEY,
    village VARCHAR(255) NOT NULL UNIQUE,
    district VARCHAR(255) NOT NULL,
    block VARCHAR(255) NOT NULL,
    risk_level VARCHAR(50) NOT NULL, -- SAFE, LOW, MODERATE, HIGH
    avg_actual_2024 DECIMAL(8,2),
    avg_predicted_2024 DECIMAL(8,2),
    avg_predicted_2025 DECIMAL(8,2),
    avg_difference DECIMAL(8,2), -- Trend indicator
    created_at TIMESTAMP DEFAULT NOW()
);
```

<br />

## 🛠️ Installation & Setup

<details>
<summary><b>1. Prerequisites</b> <i>(Click to expand)</i></summary>

- **Node.js** (v18 or higher)
- **Python** (v3.10 or higher)
- **Git**
- A **Supabase** account
- A **Groq** API Key
</details>

<details>
<summary><b>2. Clone the Repository</b> <i>(Click to expand)</i></summary>

```bash
git clone https://github.com/yourusername/Jal-Drishti.git
cd Jal-Drishti
```
</details>

<details>
<summary><b>3. Backend Setup (FastAPI)</b> <i>(Click to expand)</i></summary>

```bash
cd backend

# Create and activate virtual environment
python -m venv venv
source venv/bin/activate  # On Windows use: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Configure Environment Variables
# Create a .env file in the root backend directory:
```

**`backend/.env`** (Do NOT commit real keys)
```env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SERVICE_KEY=your_supabase_service_key
GROQ_API_KEY=your_groq_api_key
ALLOWED_ORIGINS=http://localhost:5173
```

```bash
# Run the backend server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
</details>

<details>
<summary><b>4. Frontend Setup (React/Vite)</b> <i>(Click to expand)</i></summary>

```bash
cd frontend

# Install dependencies
npm install

# Configure Environment Variables
# Create a .env.local file:
```

**`frontend/.env.local`**
```env
VITE_API_BASE_URL=http://localhost:8000
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your_supabase_anon_key
```

```bash
# Run the frontend development server
npm run dev
```
</details>

<br />

## 🌐 API Endpoints Overview

The FastAPI backend provides robust REST endpoints optimized with in-memory caching for performance.

| Endpoint | Method | Description |
| :--- | :---: | :--- |
| `/api/health` | `GET` | System health check (Supabase & Groq status). |
| `/api/cleaned/districts` | `GET` | Fetch list of available districts. |
| `/api/cleaned/villages/{district}/{block}` | `GET` | Fetch villages dynamically based on block. |
| `/api/graph-data/{village_name}` | `GET` | Unified endpoint fetching historical + predicted data. |
| `/api/chat` | `POST` | AI Chatbot interface (invokes Groq API with context). |
| `/api/village-risk/{village_name}` | `GET` | Retrieve risk level & trend analysis for a village. |

*(Detailed API documentation is auto-generated and available at `http://localhost:8000/docs` via Swagger UI).*

<br />

## 🤖 Groq AI Chatbot Architecture

Jal-Drishti's AI mentor goes beyond standard LLM wrappers. It utilizes **Retrieval-Augmented Generation (RAG)** principles to provide hyper-localized advice.

1. **Context Injection**: When a user asks a question, the backend retrieves the selected village's real-time water depth, risk level, and 5-year historical trends from Supabase.
2. **Dynamic Prompting**: This data is injected into the system prompt securely.
3. **Groq Inference**: The request is processed by `llama-3.3-70b-versatile` through the Groq Cloud API, delivering near-instantaneous responses.
4. **Multilingual Processing**: The system auto-detects English, Hindi, or Marathi, formatting the AI response naturally in the farmer's native language.

<br />

## 🤝 Contributing

We welcome contributions to make Jal Drishti even better!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<br />

<div align="center">
  <p>Made with ❤️ for Indian Farmers.</p>
  <p><b>Every drop counts! Every farmer matters! 💧🌾</b></p>
</div>
