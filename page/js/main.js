(function () {
  const MOTE_SLOTS = [
    { left: "58%", top: "18%" },
    { left: "22%", top: "42%" },
    { left: "62%", top: "48%" },
  ];

  const stage = document.getElementById("monkey-stage");
  if (!stage) return;

  const monkeyImg = document.getElementById("monkey-img");
  const hint = document.getElementById("demo-hint");
  const bananaCountEl = document.getElementById("banana-count");
  const bananaPill = document.getElementById("banana-pill");
  const toolCloth = document.getElementById("tool-cloth");

  let motesLeft = 0;
  let bananas = 0;
  let cleaned = false;

  function spawnMotes() {
    stage.querySelectorAll(".mote").forEach((el) => el.remove());
    motesLeft = MOTE_SLOTS.length;
    MOTE_SLOTS.forEach((slot) => {
      const img = document.createElement("img");
      img.src = "assets/mota_polvo.png";
      img.alt = "";
      img.className = "mote";
      img.style.left = slot.left;
      img.style.top = slot.top;
      stage.appendChild(img);
    });
  }

  function clean() {
    if (cleaned) return;
    const motes = stage.querySelectorAll(".mote");
    if (motesLeft === 0) return;

    if (toolCloth) toolCloth.classList.add("demo-tools__item--active");

    motes.forEach((mote, i) => {
      setTimeout(() => mote.classList.add("mote--gone"), i * 120);
    });

    motesLeft = 0;
    bananas += 1;
    bananaCountEl.textContent = String(bananas);
    bananaPill.classList.remove("banana-pill--pop");
    void bananaPill.offsetWidth;
    bananaPill.classList.add("banana-pill--pop");

    setTimeout(() => {
      monkeyImg.src = "assets/mono_pulcro_1.png";
      monkeyImg.alt = "Mono limpio y feliz";
      hint.textContent = "¡Así se siente en la app!";
      cleaned = true;
    }, 500);
  }

  stage.addEventListener("click", clean);
  stage.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      clean();
    }
  });

  spawnMotes();
})();
