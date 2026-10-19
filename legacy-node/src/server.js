const { conectar } = require('./db');
const { crearApp } = require('./app');

const PUERTO = 3000;

// arranca el servidor con la base de datos en el archivo citas.db
const app = crearApp(conectar('citas.db'), () => new Date());
app.listen(PUERTO, () => {
  console.log(`Servidor listo en http://localhost:${PUERTO}`);
});
