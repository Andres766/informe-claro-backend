"""Conexión a la base de datos con SQLAlchemy 2."""
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from .config import settings

connect_args = {"check_same_thread": False} if settings.database_url.startswith("sqlite") else {}
engine = create_engine(settings.database_url, connect_args=connect_args)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# (tabla, columna, definición SQL) de columnas agregadas después del primer despliegue.
_COLUMNAS_NUEVAS = [
    ("usuario", "password_hash", "VARCHAR(100)"),
    ("informe_cargado", "idioma", "VARCHAR(2) NOT NULL DEFAULT 'es'"),
]


def migrar_columnas_nuevas():
    """Añade columnas que se agregaron al modelo después de crear las tablas.

    create_all() no modifica tablas existentes (p. ej. la BD ya desplegada en Render).
    """
    from sqlalchemy import inspect, text

    inspector = inspect(engine)
    for tabla, columna, definicion in _COLUMNAS_NUEVAS:
        if columna not in {c["name"] for c in inspector.get_columns(tabla)}:
            with engine.begin() as conn:
                conn.execute(text(f"ALTER TABLE {tabla} ADD COLUMN {columna} {definicion}"))
