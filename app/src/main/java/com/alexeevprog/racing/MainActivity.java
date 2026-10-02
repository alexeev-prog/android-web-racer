package com.alexeevprog.racing;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private ValueCallback<Uri[]> filePathCallback;

    private static final String HTML_CONTENT = """
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover">
<meta name="theme-color" content="#06060d">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<title>Neon Racer</title>
<style>
  *{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}
  html,body{
    width:100%;height:100%;overflow:hidden;background:#06060d;
    font-family:system-ui,-apple-system,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif;
    -webkit-user-select:none;user-select:none;overscroll-behavior:none;
  }
  #game{position:fixed;inset:0;overflow:hidden;touch-action:none;background:#06060d}
  #cv{position:absolute;inset:0;width:100%;height:100%;display:block}

  /* ---------- HUD ---------- */
  .hud{
    position:absolute;top:0;left:0;right:0;z-index:5;
    display:flex;justify-content:space-between;align-items:flex-start;
    padding:calc(env(safe-area-inset-top,0px) + 16px)
            calc(env(safe-area-inset-right,0px) + 20px) 0
            calc(env(safe-area-inset-left,0px) + 20px);
    pointer-events:none;opacity:0;transition:opacity .3s ease;
  }
  .hud.show{opacity:1}
  .hud-box{display:flex;flex-direction:column;gap:2px}
  .hud-box.right{align-items:flex-end}
  .hud-label{
    font-size:10px;font-weight:800;letter-spacing:.2em;
    color:rgba(255,255,255,.42);
  }
  .hud-value{
    font-size:27px;font-weight:800;line-height:1.05;color:#fff;
    font-variant-numeric:tabular-nums;
    text-shadow:0 0 16px rgba(0,229,255,.55);
  }
  .hud-box.right .hud-value{text-shadow:0 0 16px rgba(255,45,85,.55)}

  /* ---------- Screens ---------- */
  .screen{
    position:absolute;inset:0;z-index:10;
    display:flex;align-items:center;justify-content:center;padding:22px;
    background:radial-gradient(circle at 50% 38%,rgba(12,14,34,.72),rgba(3,3,8,.94));
    -webkit-backdrop-filter:blur(7px);backdrop-filter:blur(7px);
    opacity:0;pointer-events:none;transition:opacity .35s ease;
  }
  .screen.show{opacity:1;pointer-events:auto}

  .panel{
    width:min(340px,88vw);
    padding:34px 24px 30px;
    border-radius:30px;
    display:flex;flex-direction:column;align-items:center;gap:14px;
    background:linear-gradient(160deg,rgba(30,34,62,.94),rgba(11,13,26,.97));
    border:1px solid rgba(0,229,255,.22);
    box-shadow:
      0 0 70px rgba(0,229,255,.13),
      0 26px 60px rgba(0,0,0,.65),
      inset 0 1px 0 rgba(255,255,255,.09);
    transform:scale(.93);
    transition:transform .4s cubic-bezier(.2,.9,.3,1.25);
  }
  .screen.show .panel{transform:scale(1)}

  .title{
    font-size:clamp(34px,11.5vw,54px);font-weight:900;line-height:.94;
    letter-spacing:.06em;text-align:center;
    background:linear-gradient(180deg,#ffffff 10%,#00e5ff 95%);
    -webkit-background-clip:text;background-clip:text;color:transparent;
    filter:drop-shadow(0 0 22px rgba(0,229,255,.5));
  }
  .over-title{
    font-size:clamp(26px,8.5vw,38px);font-weight:900;letter-spacing:.12em;
    background:linear-gradient(180deg,#ffffff 10%,#ff2d55 95%);
    -webkit-background-clip:text;background-clip:text;color:transparent;
    filter:drop-shadow(0 0 22px rgba(255,45,85,.5));
  }
  .sub{
    font-size:13px;line-height:1.5;text-align:center;font-weight:600;
    color:rgba(255,255,255,.5);max-width:250px;
  }
  .hint{
    display:flex;align-items:center;gap:8px;margin-top:2px;
    font-size:11px;font-weight:700;letter-spacing:.08em;
    color:rgba(255,255,255,.32);
  }
  .hint svg{width:16px;height:16px;flex:none;opacity:.6}

  .btn{
    margin-top:6px;padding:16px 46px;border:0;border-radius:999px;
    font-family:inherit;font-size:17px;font-weight:900;letter-spacing:.14em;
    color:#04141c;cursor:pointer;
    background:linear-gradient(135deg,#00e5ff 0%,#5cffd0 100%);
    box-shadow:0 0 30px rgba(0,229,255,.5),0 10px 26px rgba(0,0,0,.5);
    transition:transform .12s ease,box-shadow .2s ease;
  }
  .btn:active{transform:scale(.93);box-shadow:0 0 20px rgba(0,229,255,.85),0 4px 14px rgba(0,0,0,.5)}

  .best{
    font-size:12px;font-weight:700;letter-spacing:.1em;
    color:rgba(255,255,255,.4);
  }
  .best b{color:#00e5ff;font-weight:900}

  .stats{
    display:flex;gap:14px;width:100%;margin:6px 0 2px;
  }
  .stat{
    flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;
    padding:14px 8px;border-radius:18px;
    background:rgba(255,255,255,.045);
    border:1px solid rgba(255,255,255,.07);
  }
  .stat span{
    font-size:9.5px;font-weight:800;letter-spacing:.18em;
    color:rgba(255,255,255,.4);
  }
  .stat b{
    font-size:26px;font-weight:900;color:#fff;
    font-variant-numeric:tabular-nums;line-height:1.1;
  }
  .stat b.record{color:#ffd54a;text-shadow:0 0 18px rgba(255,213,74,.65)}
</style>
</head>
<body>
<div id="game">
  <canvas id="cv"></canvas>

  <div class="hud" id="hud">
    <div class="hud-box">
      <span class="hud-label">ОЧКИ</span>
      <span class="hud-value" id="scoreEl">0</span>
    </div>
    <div class="hud-box right">
      <span class="hud-label">КМ/Ч</span>
      <span class="hud-value" id="speedEl">0</span>
    </div>
  </div>

  <div class="screen show" id="startScreen">
    <div class="panel">
      <h1 class="title">NEON<br>RACER</h1>
      <p class="sub">Уклоняйся от машин и держись как можно дольше</p>
      <button class="btn" id="playBtn">ИГРАТЬ</button>
      <div class="best">РЕКОРД: <b id="bestStart">0</b></div>
      <div class="hint">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M9 8l-4 4 4 4M15 8l4 4-4 4"/></svg>
        ВОДИ ПАЛЬЦЕМ ПО ЭКРАНУ
      </div>
    </div>
  </div>

  <div class="screen" id="overScreen">
    <div class="panel">
      <h2 class="over-title">АВАРИЯ</h2>
      <div class="stats">
        <div class="stat"><span>ОЧКИ</span><b id="finalScore">0</b></div>
        <div class="stat"><span>РЕКОРД</span><b id="bestScore">0</b></div>
      </div>
      <button class="btn" id="retryBtn">ЗАНОВО</button>
    </div>
  </div>
</div>

<script>
(function () {
  'use strict';

  /* ================= DOM ================= */
  const gameEl      = document.getElementById('game');
  const cv          = document.getElementById('cv');
  const ctx         = cv.getContext('2d');
  const hudEl       = document.getElementById('hud');
  const scoreEl     = document.getElementById('scoreEl');
  const speedEl     = document.getElementById('speedEl');
  const startScreen = document.getElementById('startScreen');
  const overScreen  = document.getElementById('overScreen');
  const playBtn     = document.getElementById('playBtn');
  const retryBtn    = document.getElementById('retryBtn');
  const bestStartEl = document.getElementById('bestStart');
  const finalScoreEl= document.getElementById('finalScore');
  const bestScoreEl = document.getElementById('bestScore');

  /* ================= Константы ================= */
  const LANES = 3;
  const clamp = (v, a, b) => (v < a ? a : v > b ? b : v);

  const PLAYER_PAL = { main:'#00e5ff', dark:'#006b8a', roof:'#06384a' };
  const ENEMY_PALS = [
    { main:'#ff2d55', dark:'#8f0026', roof:'#4d0014' },
    { main:'#ffb300', dark:'#946200', roof:'#4d3300' },
    { main:'#a259ff', dark:'#4f2394', roof:'#2a1152' },
    { main:'#2bd97c', dark:'#0f7a41', roof:'#08411f' },
    { main:'#ff7a18', dark:'#94430a', roof:'#4d2205' },
    { main:'#ff4fd8', dark:'#94088a', roof:'#4d0447' }
  ];
  const PARTICLE_COLORS = ['#fff3b0', '#ffb300', '#ff6a00', '#ff2d55', '#ffffff'];

  /* ================= Состояние ================= */
  let W = 0, H = 0, DPR = 1;
  let carW = 60, carH = 110;
  const road = { left:0, right:0, width:0, laneW:0 };

  let bgGrad = null, roadGrad = null, vignetteGrad = null;

  const state = {
    mode: 'start',        // start | playing | over
    score: 0,
    time: 0,
    speed: 0,
    scroll: 0,
    spawnTimer: 1,
    shake: 0,
    flash: 0,
    playerVisible: true,
    best: 0
  };

  const player = { x:0, y:0, targetX:0 };
  const enemies = [];
  const particles = [];
  let lastRowEnemies = [];
  let overTimer = null;

  /* ================= Утилиты ================= */
  function roundRect(c, x, y, w, h, r) {
    r = Math.min(r, Math.abs(w) / 2, Math.abs(h) / 2);
    c.beginPath();
    c.moveTo(x + r, y);
    c.lineTo(x + w - r, y);
    c.quadraticCurveTo(x + w, y, x + w, y + r);
    c.lineTo(x + w, y + h - r);
    c.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    c.lineTo(x + r, y + h);
    c.quadraticCurveTo(x, y + h, x, y + h - r);
    c.lineTo(x, y + r);
    c.quadraticCurveTo(x, y, x + r, y);
    c.closePath();
  }

  function laneCenter(i) {
    return road.left + road.laneW * (i + 0.5);
  }

  /* ================= Размеры / раскладка ================= */
  function resize() {
    const rect = gameEl.getBoundingClientRect();
    W = Math.max(1, rect.width);
    H = Math.max(1, rect.height);
    DPR = Math.min(window.devicePixelRatio || 1, 2);

    cv.width  = Math.round(W * DPR);
    cv.height = Math.round(H * DPR);

    ctx.setTransform(DPR, 0, 0, DPR, 0, 0);

    layout();
    buildGradients();
  }

  function layout() {
    const maxRoadW = Math.min(W * 0.90, H * 0.70);
    road.width = maxRoadW;
    road.left  = (W - maxRoadW) / 2;
    road.right = road.left + maxRoadW;
    road.laneW = road.width / LANES;

    carW = Math.min(road.laneW * 0.62, H * 0.085);
    carH = carW * 1.85;

    player.y = H - carH / 2 - Math.max(H * 0.11, 64);

    const minX = road.left + carW / 2;
    const maxX = road.right - carW / 2;
    player.x = clamp(player.x || (road.left + road.width / 2), minX, maxX);
    player.targetX = clamp(player.targetX || player.x, minX, maxX);

    // пересчёт врагов по новым полосам
    for (const e of enemies) e.x = laneCenter(e.lane);
  }

  function buildGradients() {
    bgGrad = ctx.createLinearGradient(0, 0, 0, H);
    bgGrad.addColorStop(0, '#07070f');
    bgGrad.addColorStop(0.55, '#0a0918');
    bgGrad.addColorStop(1, '#0e0b20');

    roadGrad = ctx.createLinearGradient(road.left, 0, road.right, 0);
    roadGrad.addColorStop(0,    '#15151f');
    roadGrad.addColorStop(0.5,  '#1f1f2c');
    roadGrad.addColorStop(1,    '#15151f');

    vignetteGrad = ctx.createRadialGradient(
      W / 2, H / 2, Math.min(W, H) * 0.30,
      W / 2, H / 2, Math.max(W, H) * 0.78
    );
    vignetteGrad.addColorStop(0, 'rgba(0,0,0,0)');
    vignetteGrad.addColorStop(1, 'rgba(0,0,0,0.55)');
  }

  /* ================= Спавн ================= */
  function rowCleared() {
    for (let i = 0; i < lastRowEnemies.length; i++) {
      const e = lastRowEnemies[i];
      if (!e.removed && e.y < carH * 2.6) return false;
    }
    return true;
  }

  function spawnRow() {
    const order = [0, 1, 2];
    for (let i = order.length - 1; i > 0; i--) {
      const j = (Math.random() * (i + 1)) | 0;
      const t = order[i]; order[i] = order[j]; order[j] = t;
    }

    // На высокой сложности чаще появляются двойные ряды
    const doubleChance = clamp(0.18 + state.time * 0.006, 0.18, 0.52);
    const count = (Math.random() < doubleChance) ? 2 : 1;

    const speedFactor = 0.42 + Math.random() * 0.26; // одинаковый для всего ряда
    const pal = ENEMY_PALS[(Math.random() * ENEMY_PALS.length) | 0];
    const baseY = -carH * 0.9 - Math.random() * carH * 0.35;

    lastRowEnemies = [];
    for (let i = 0; i < count; i++) {
      const lane = order[i];
      const e = {
        lane: lane,
        x: laneCenter(lane),
        y: baseY,
        speedFactor: speedFactor,
        pal: pal,
        removed: false
      };
      enemies.push(e);
      lastRowEnemies.push(e);
    }
  }

  /* ================= Частицы ================= */
  function spawnExplosion(x, y) {
    for (let i = 0; i < 34; i++) {
      const a = Math.random() * Math.PI * 2;
      const sp = 50 + Math.random() * 340;
      particles.push({
        x: x, y: y,
        vx: Math.cos(a) * sp,
        vy: Math.sin(a) * sp,
        t: 0,
        life: 0.45 + Math.random() * 0.55,
        size: 1.8 + Math.random() * 4.2,
        color: PARTICLE_COLORS[(Math.random() * PARTICLE_COLORS.length) | 0]
      });
    }
  }

  function updateParticles(dt) {
    for (let i = particles.length - 1; i >= 0; i--) {
      const p = particles[i];
      p.t += dt;
      if (p.t >= p.life) { particles.splice(i, 1); continue; }
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vx *= (1 - 2.4 * dt);
      p.vy *= (1 - 2.4 * dt);
      p.vy += 90 * dt;
    }
  }

  function drawParticles() {
    for (let i = 0; i < particles.length; i++) {
      const p = particles[i];
      const k = 1 - p.t / p.life;
      ctx.globalAlpha = k * k;
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size * (0.4 + k * 0.6), 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalAlpha = 1;
  }

  /* ================= Отрисовка машины ================= */
  function drawCar(cx, cy, w, h, pal, isPlayer) {
    const x = cx - w / 2;
    const y = cy - h / 2;

    // тень
    ctx.globalAlpha = 0.42;
    ctx.fillStyle = '#000';
    ctx.beginPath();
    ctx.ellipse(cx, cy + h * 0.09, w * 0.56, h * 0.46, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = 1;

    // колёса
    const ww = w * 0.15, wh = h * 0.20;
    ctx.fillStyle = '#07070c';
    roundRect(ctx, x - ww * 0.45, y + h * 0.13, ww, wh, ww * 0.45); ctx.fill();
    roundRect(ctx, x + w - ww * 0.55, y + h * 0.13, ww, wh, ww * 0.45); ctx.fill();
    roundRect(ctx, x - ww * 0.45, y + h * 0.67, ww, wh, ww * 0.45); ctx.fill();
    roundRect(ctx, x + w - ww * 0.55, y + h * 0.67, ww, wh, ww * 0.45); ctx.fill();

    // свечение корпуса (только игрок — дорого по производительности)
    if (isPlayer) {
      ctx.save();
      ctx.shadowColor = pal.main;
      ctx.shadowBlur = w * 1.05;
      ctx.fillStyle = pal.main;
      roundRect(ctx, x, y, w, h, w * 0.26);
      ctx.fill();
      ctx.restore();
    }

    // корпус
    const bg = ctx.createLinearGradient(x, 0, x + w, 0);
    bg.addColorStop(0,    pal.dark);
    bg.addColorStop(0.24, pal.main);
    bg.addColorStop(0.5,  pal.main);
    bg.addColorStop(0.76, pal.main);
    bg.addColorStop(1,    pal.dark);
    ctx.fillStyle = bg;
    roundRect(ctx, x, y, w, h, w * 0.26);
    ctx.fill();

    // объём
    const hg = ctx.createLinearGradient(x, y, x, y + h);
    hg.addColorStop(0,    'rgba(255,255,255,0.30)');
    hg.addColorStop(0.32, 'rgba(255,255,255,0.04)');
    hg.addColorStop(1,    'rgba(0,0,0,0.28)');
    ctx.fillStyle = hg;
    roundRect(ctx, x, y, w, h, w * 0.26);
    ctx.fill();

    // лобовое стекло
    ctx.fillStyle = 'rgba(8,12,22,0.92)';
    roundRect(ctx, x + w * 0.14, y + h * 0.155, w * 0.72, h * 0.185, w * 0.09);
    ctx.fill();

    // крыша
    ctx.fillStyle = pal.roof;
    roundRect(ctx, x + w * 0.17, y + h * 0.375, w * 0.66, h * 0.215, w * 0.08);
    ctx.fill();

    // заднее стекло
    ctx.fillStyle = 'rgba(8,12,22,0.92)';
    roundRect(ctx, x + w * 0.17, y + h * 0.615, w * 0.66, h * 0.14, w * 0.08);
    ctx.fill();

    // фары
    ctx.fillStyle = '#fffbe0';
    roundRect(ctx, x + w * 0.13, y + h * 0.035, w * 0.20, h * 0.035, w * 0.03); ctx.fill();
    roundRect(ctx, x + w * 0.67, y + h * 0.035, w * 0.20, h * 0.035, w * 0.03); ctx.fill();

    // стоп-сигналы
    ctx.fillStyle = '#ff3355';
    roundRect(ctx, x + w * 0.11, y + h * 0.925, w * 0.24, h * 0.040, w * 0.03); ctx.fill();
    roundRect(ctx, x + w * 0.65, y + h * 0.925, w * 0.24, h * 0.040, w * 0.03); ctx.fill();
  }

  /* ================= Отрисовка трассы ================= */
  function drawRoad() {
    // фон
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, W, H);

    // полотно
    ctx.fillStyle = roadGrad;
    ctx.fillRect(road.left, 0, road.width, H);

    // разметка полос
    const dashLen = carH * 0.42;
    const period  = dashLen * 2;
    const off     = state.scroll % period;
    const lw      = Math.max(2, W * 0.006);

    ctx.fillStyle = 'rgba(255,255,255,0.20)';
    for (let i = 1; i < LANES; i++) {
      const lx = road.left + road.laneW * i - lw / 2;
      for (let y = -period + off; y < H; y += period) {
        roundRect(ctx, lx, y, lw, dashLen, lw / 2);
        ctx.fill();
      }
    }

    // неоновые борта
    const edgeW = Math.max(3, W * 0.008);

    ctx.fillStyle = 'rgba(0,229,255,0.10)';
    ctx.fillRect(road.left - edgeW * 3, 0, edgeW * 3, H);
    ctx.fillRect(road.right, 0, edgeW * 3, H);

    ctx.fillStyle = '#00e5ff';
    ctx.fillRect(road.left - edgeW, 0, edgeW, H);
    ctx.fillRect(road.right, 0, edgeW, H);

    // бегущие метки по бортам
    const mPeriod = carH * 1.6;
    const mOff = state.scroll % mPeriod;
    ctx.fillStyle = 'rgba(0,229,255,0.55)';
    for (let y = -mPeriod + mOff; y < H; y += mPeriod) {
      ctx.fillRect(road.left - edgeW * 2.6, y, edgeW * 1.6, carH * 0.42);
      ctx.fillRect(road.right + edgeW, y, edgeW * 1.6, carH * 0.42);
    }
  }

  /* ================= Отрисовка кадра ================= */
  function render() {
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
    ctx.clearRect(0, 0, W, H);

    ctx.save();
    if (state.shake > 0) {
      const s = state.shake * 16;
      ctx.translate((Math.random() - 0.5) * s, (Math.random() - 0.5) * s);
    }

    drawRoad();

    for (let i = 0; i < enemies.length; i++) {
      const e = enemies[i];
      drawCar(e.x, e.y, carW, carH, e.pal, false);
    }

    if (state.playerVisible) {
      drawCar(player.x, player.y, carW, carH, PLAYER_PAL, true);
    }

    drawParticles();

    ctx.restore();

    // виньетка
    ctx.fillStyle = vignetteGrad;
    ctx.fillRect(0, 0, W, H);

    // вспышка при аварии
    if (state.flash > 0) {
      ctx.globalAlpha = state.flash * 0.65;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, W, H);
      ctx.globalAlpha = 1;
    }
  }

  /* ================= Логика ================= */
  function hits(a, b) {
    return Math.abs(a.x - b.x) < carW * 0.82 &&
           Math.abs(a.y - b.y) < carH * 0.84;
  }

  function update(dt) {
    updateParticles(dt);

    if (state.shake > 0) state.shake = Math.max(0, state.shake - dt * 2.6);
    if (state.flash > 0) state.flash = Math.max(0, state.flash - dt * 2.2);

    if (state.mode === 'start') {
      state.speed = 250;
      state.scroll += state.speed * dt;
      return;
    }

    if (state.mode === 'over') return;

    /* ---- playing ---- */
    state.time += dt;

    const t = Math.min(state.time / 70, 1);
    const ease = t * t * (3 - 2 * t);
    state.speed = 330 + (1000 - 330) * ease;

    state.score  += state.speed * dt * 0.06;
    state.scroll += state.speed * dt;

    // плавное движение игрока (не зависит от FPS)
    const k = 1 - Math.exp(-18 * dt);
    player.x += (player.targetX - player.x) * k;

    // спавн рядов
    state.spawnTimer -= dt;
    if (state.spawnTimer <= 0 && rowCleared()) {
      spawnRow();
      state.spawnTimer = 0.12 + Math.random() * 0.30;
    }

    // враги
    for (let i = enemies.length - 1; i >= 0; i--) {
      const e = enemies[i];
      e.y += state.speed * (1 - e.speedFactor) * dt;

      if (e.y - carH > H) {
        e.removed = true;
        enemies.splice(i, 1);
        continue;
      }

      if (state.playerVisible && hits(e, player)) {
        gameOver();
        return;
      }
    }
  }

  /* ================= HUD ================= */
  let lastScoreShown = -1;
  let lastSpeedShown = -1;

  function updateHud() {
    const s = Math.floor(state.score);
    if (s !== lastScoreShown) {
      scoreEl.textContent = s;
      lastScoreShown = s;
    }
    const kmh = state.mode === 'playing' ? Math.round(state.speed / 3.6) : 0;
    if (kmh !== lastSpeedShown) {
      speedEl.textContent = kmh;
      lastSpeedShown = kmh;
    }
  }

  /* ================= Игровой цикл ================= */
  let lastTs = 0;

  function loop(ts) {
    if (!lastTs) lastTs = ts;
    let dt = (ts - lastTs) / 1000;
    lastTs = ts;
    if (dt > 0.05) dt = 0.05;
    if (dt < 0) dt = 0;

    update(dt);
    render();
    updateHud();

    requestAnimationFrame(loop);
  }

  /* ================= Управление ================= */
  let pointerId = null;
  let dragging = false;
  let dragStartX = 0;
  let dragStartCarX = 0;

  function pointerPos(e) {
    return { x: e.clientX, y: e.clientY };
  }

  function onDown(e) {
    if (state.mode !== 'playing') return;
    if (pointerId !== null) return;
    pointerId = e.pointerId;
    dragging = true;
    const p = pointerPos(e);
    dragStartX = p.x;
    dragStartCarX = player.x;
    if (gameEl.setPointerCapture) {
      try { gameEl.setPointerCapture(e.pointerId); } catch (err) {}
    }
    e.preventDefault();
  }

  function onMove(e) {
    if (!dragging || e.pointerId !== pointerId) return;
    const p = pointerPos(e);
    const dx = (p.x - dragStartX) * 1.12;
    player.targetX = clamp(
      dragStartCarX + dx,
      road.left + carW / 2,
      road.right - carW / 2
    );
    e.preventDefault();
  }

  function onUp(e) {
    if (e.pointerId !== pointerId) return;
    dragging = false;
    pointerId = null;
  }

  gameEl.addEventListener('pointerdown', onDown, { passive: false });
  gameEl.addEventListener('pointermove', onMove, { passive: false });
  gameEl.addEventListener('pointerup', onUp);
  gameEl.addEventListener('pointercancel', onUp);
  gameEl.addEventListener('lostpointercapture', onUp);

  // Клавиатура (для десктопа)
  const keys = { left: false, right: false };
  function keyboardSteer() {
    if (state.mode !== 'playing') return;
    const step = road.laneW * 0.9;
    if (keys.left && !keys.right) {
      player.targetX = clamp(player.x - step, road.left + carW / 2, road.right - carW / 2);
      keys.left = false;
    } else if (keys.right && !keys.left) {
      player.targetX = clamp(player.x + step, road.left + carW / 2, road.right - carW / 2);
      keys.right = false;
    }
  }
  window.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowLeft' || e.key === 'a' || e.key === 'A') { keys.left = true; keyboardSteer(); }
    if (e.key === 'ArrowRight' || e.key === 'd' || e.key === 'D') { keys.right = true; keyboardSteer(); }
    if (e.key === ' ' && state.mode === 'over') { retryBtn.click(); }
  });
  window.addEventListener('keyup', (e) => {
    if (e.key === 'ArrowLeft' || e.key === 'a' || e.key === 'A') keys.left = false;
    if (e.key === 'ArrowRight' || e.key === 'd' || e.key === 'D') keys.right = false;
  });

  /* ================= Переходы состояний ================= */
  function resetGame() {
    state.mode = 'playing';
    state.score = 0;
    state.time = 0;
    state.speed = 330;
    state.scroll = 0;
    state.spawnTimer = 0.8;
    state.shake = 0;
    state.flash = 0;
    state.playerVisible = true;

    enemies.length = 0;
    particles.length = 0;
    lastRowEnemies = [];

    player.x = road.left + road.width / 2;
    player.targetX = player.x;

    dragging = false;
    pointerId = null;

    lastScoreShown = -1;
    lastSpeedShown = -1;
  }

  function startGame() {
    clearTimeout(overTimer);
    startScreen.classList.remove('show');
    overScreen.classList.remove('show');
    hudEl.classList.add('show');
    resetGame();
  }

  function gameOver() {
    state.mode = 'over';
    state.playerVisible = false;
    state.shake = 1;
    state.flash = 1;
    state.speed = 0;

    spawnExplosion(player.x, player.y);

    if (navigator.vibrate) {
      try { navigator.vibrate(70); } catch (err) {}
    }

    const s = Math.floor(state.score);
    const isRecord = s > state.best;
    if (isRecord) {
      state.best = s;
      try { localStorage.setItem('neonRacerBest', String(s)); } catch (err) {}
    }

    finalScoreEl.textContent = s;
    bestScoreEl.textContent = state.best;
    bestScoreEl.classList.toggle('record', isRecord);

    hudEl.classList.remove('show');

    clearTimeout(overTimer);
    overTimer = setTimeout(() => {
      if (state.mode === 'over') overScreen.classList.add('show');
    }, 520);
  }

  playBtn.addEventListener('click', startGame);
  retryBtn.addEventListener('click', startGame);

  /* ================= Инициализация ================= */
  function loadBest() {
    let v = 0;
    try { v = parseInt(localStorage.getItem('neonRacerBest') || '0', 10) || 0; } catch (err) { v = 0; }
    state.best = v;
    bestStartEl.textContent = v;
  }

  function onResize() {
    resize();
  }

  window.addEventListener('resize', onResize);
  window.addEventListener('orientationchange', () => {
    setTimeout(onResize, 180);
  });

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      lastTs = 0;
      dragging = false;
      pointerId = null;
    }
  });

  // предотвращаем прокрутку / зум
  document.addEventListener('gesturestart', (e) => e.preventDefault());
  document.addEventListener('touchmove', (e) => {
    if (e.target === cv) e.preventDefault();
  }, { passive: false });

  loadBest();
  resize();
  requestAnimationFrame(loop);
})();
</script>
</body>
</html>
""";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createWebView();
    }

    private void createWebView() {
        WebView wv = new WebView(this);

        android.webkit.WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

        try {
            java.io.File dir = getDir("webview", android.content.Context.MODE_PRIVATE);
            if (!dir.exists()) dir.mkdirs();
            s.setDatabasePath(dir.getAbsolutePath());
            android.webkit.WebStorage.getInstance().setQuotaForOrigin("file:///", 200L * 1024L * 1024L);
        } catch (Exception ignored) {}

        wv.addJavascriptInterface(new AndroidFileSaver(), "AndroidFileSaver");
        wv.addJavascriptInterface(new AndroidStorage(), "AndroidStorage");

        wv.setWebViewClient(new WebViewClient());

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                MainActivity.this.filePathCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        wv.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {}
        });

        wv.loadDataWithBaseURL(null, HTML_CONTENT, "text/html", "UTF-8", null);
        setContentView(wv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{ data.getData() };
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    /**
     * Мост для сохранения файлов из JavaScript.
     * Принимает dataURL "data:mime;base64,..." и сохраняет в Downloads.
     */
    public class AndroidFileSaver {
        @JavascriptInterface
        public void saveBase64(final String dataUrl, final String suggestedName, final String mimetype) {
            new Thread(() -> {
                try {
                    int comma = dataUrl.indexOf(',');
                    if (comma < 0) {
                        showToast("Неверный формат данных");
                        return;
                    }
                    String b64 = dataUrl.substring(comma + 1);
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);

                    String fileName = (suggestedName == null || suggestedName.isEmpty())
                        ? "download_" + System.currentTimeMillis()
                        : suggestedName;
                    String mime = (mimetype == null || mimetype.isEmpty())
                        ? "application/octet-stream"
                        : mimetype;

                    if (Build.VERSION.SDK_INT >= 29) {
                        // Android 10+ через MediaStore (Downloads)
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                        Uri uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri != null) {
                            OutputStream os = getContentResolver().openOutputStream(uri);
                            if (os != null) {
                                os.write(bytes);
                                os.close();
                                showToast("Сохранено в Downloads: " + fileName);
                                return;
                            }
                        }
                        showToast("Не удалось сохранить");
                    } else {
                        // Android 9 и ниже — напрямую в папку
                        File downloads = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                        if (!downloads.exists()) downloads.mkdirs();
                        File out = new File(downloads, fileName);
                        FileOutputStream fos = new FileOutputStream(out);
                        fos.write(bytes);
                        fos.close();
                        showToast("Сохранено: " + out.getAbsolutePath());
                    }
                } catch (Exception e) {
                    showToast("Ошибка: " + e.getMessage());
                }
            }).start();
        }
    }

    /**
     * Мост для постоянного хранения данных из JavaScript.
     * SharedPreferences — данные не стираются при закрытии приложения.
     */
    public class AndroidStorage {
        private SharedPreferences prefs;

        AndroidStorage() {
            prefs = getSharedPreferences("apkb_storage", Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public String get(String key) {
            try { return prefs.getString(key, null); } catch (Exception e) { return null; }
        }

        @JavascriptInterface
        public void set(String key, String value) {
            try { prefs.edit().putString(key, value).apply(); } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void remove(String key) {
            try { prefs.edit().remove(key).apply(); } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public String keys() {
            try {
                JSONArray arr = new JSONArray();
                for (String k : prefs.getAll().keySet()) arr.put(k);
                return arr.toString();
            } catch (Exception e) {
                return "[]";
            }
        }
    }

    private void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
    }
}
