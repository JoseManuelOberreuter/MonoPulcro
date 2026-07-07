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
  const cleanFx = document.getElementById("clean-fx");

  let motesLeft = 0;
  let bananas = 0;
  let cleaned = false;
  let cleaning = false;

  function spawnMotes() {
    stage.querySelectorAll(".mote").forEach((el) => el.remove());
    motesLeft = MOTE_SLOTS.length;
    MOTE_SLOTS.forEach((slot, index) => {
      const img = document.createElement("img");
      img.src = "assets/mota_polvo.png";
      img.alt = "";
      img.className = "mote";
      img.style.left = slot.left;
      img.style.top = slot.top;
      img.style.animationDelay = `${-0.7 * index}s`;
      stage.appendChild(img);
    });
  }

  function resetCleanFx() {
    if (!cleanFx) return;
    cleanFx.classList.remove("clean-fx--active", "clean-fx--wipe");
  }

  function clean() {
    if (cleaned || cleaning) return;
    if (motesLeft === 0) return;

    cleaning = true;
    stage.style.pointerEvents = "none";

    if (cleanFx) {
      resetCleanFx();
      void cleanFx.offsetWidth;
      cleanFx.classList.add("clean-fx--active");
      setTimeout(() => cleanFx.classList.add("clean-fx--wipe"), 350);
    }

    const motes = stage.querySelectorAll(".mote");
    motes.forEach((mote, i) => {
      setTimeout(() => mote.classList.add("mote--gone"), 400 + i * 120);
    });

    motesLeft = 0;

    setTimeout(() => {
      monkeyImg.src = "assets/mono_pulcro_1.png";
      monkeyImg.alt = "Mono limpio y feliz";
      resetCleanFx();

      bananas += 1;
      bananaCountEl.textContent = String(bananas);
      bananaPill.classList.remove("banana-pill--pop");
      void bananaPill.offsetWidth;
      bananaPill.classList.add("banana-pill--pop");

      hint.textContent = "¡Así se siente en la app!";
      cleaned = true;
      cleaning = false;
    }, 1100);
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
