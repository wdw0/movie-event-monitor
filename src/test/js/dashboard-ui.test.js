const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const recommendations = require('../../main/resources/dashboard/recommendations.js');

function startDashboard() {
  const html = fs.readFileSync('src/main/resources/dashboard/index.html', 'utf8');
  const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
  const elements = new Map();
  const tabs = ['dashboard-view', 'recommended-view'].map(id => ({
    id,
    dataset: { tab: id },
    attributes: {},
    setAttribute(name, value) { this.attributes[name] = value; }
  }));
  const views = tabs.map(({ id }) => ({
    id,
    classes: new Set(),
    classList: { toggle(name, force) { force ? this.owner.classes.add(name) : this.owner.classes.delete(name); } }
  }));
  views.forEach(view => { view.classList.owner = view; });
  const intro = { textContent: '' };
  const getElementById = id => {
    if (!elements.has(id)) {
      elements.set(id, {
        innerHTML: '', textContent: '', className: '',
        querySelector(selector) { return selector === '.recommend-intro' ? intro : null; }
      });
    }
    return elements.get(id);
  };
  let clickHandler;
  const document = {
    getElementById,
    addEventListener(type, handler) { if (type === 'click') clickHandler = handler; },
    querySelectorAll(selector) {
      if (selector === '[data-tab]') return tabs;
      if (selector === '#dashboard-view,#recommended-view') return views;
      return [];
    }
  };
  const storage = new Map();
  const snapshot = {
    data: {
      moviesTracked: 2, events: 2, trending: 0, newMovies: 2,
      movies: [
        { movieId: 1, title: 'Liked Film', rating: 8, votes: 10, popularity: 20, genres: ['Science Fiction'], trending: false },
        { movieId: 2, title: 'Suggestion', rating: 7, votes: 5, popularity: 12, genres: ['Science Fiction'], trending: false }
      ],
      recentEvents: [], eventCounts: {}, popularityHistory: []
    },
    kafka: { online: true, brokers: [], topics: [], consumerGroups: {} }
  };
  vm.runInNewContext(script, {
    document,
    localStorage: {
      getItem(key) { return storage.get(key) ?? null; },
      setItem(key, value) { storage.set(key, value); }
    },
    MovieRecommendations: recommendations,
    fetch: async () => ({ ok: true, json: async () => snapshot }),
    setInterval() {}
  });
  return { elements, storage, get clickHandler() { return clickHandler; }, tabs, views, intro };
}

test('like buttons persist locally and update recommendations and tab selection', async () => {
  const app = startDashboard();
  await new Promise(resolve => setImmediate(resolve));
  assert.match(app.elements.get('movie-rows').innerHTML, /data-like="1"/);
  assert.match(app.intro.textContent, /Curta alguns filmes/);

  app.clickHandler({ target: { closest(selector) {
    return selector === '[data-like]' ? { dataset: { like: '1' } } : null;
  } } });
  assert.equal(app.storage.get('movie-monitor-liked-v1'), '[1]');
  assert.match(app.elements.get('recommendations').innerHTML, /Suggestion/);
  assert.match(app.elements.get('recommendations').innerHTML, /Mesmo gênero de Liked Film/);

  app.clickHandler({ target: { closest(selector) {
    return selector === '[data-tab]' ? app.tabs[1] : null;
  } } });
  assert.equal(app.tabs[1].attributes['aria-selected'], 'true');
  assert.equal(app.views[0].classes.has('hidden'), true);
  assert.equal(app.views[1].classes.has('hidden'), false);
});
