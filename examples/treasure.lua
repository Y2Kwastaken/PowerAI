-- treasure: buries a gold block somewhere near you. Dig it up before time runs out.
-- The action bar tells you how warm you are and a geiger counter clicks faster the closer you get.
-- Works best in survival on natural ground. If time runs out the block is put back the way it was.

local MIN_DISTANCE = 12   -- how far away the treasure can be, in blocks
local MAX_DISTANCE = 28
local TIME_LIMIT = 90     -- seconds
local HINT_AT = 45        -- seconds left when a hint appears

local REWARDS = {
  { item = "diamond", count = 3 },
  { item = "emerald", count = 8 },
  { item = "golden_apple", count = 2 },
  { item = "ender_pearl", count = 4 },
  { item = "experience_bottle", count = 16 },
}

local function isAir(name)
  return name == nil or name == "air" or name == "cave_air" or name == "void_air"
end

-- surfaces we can't or shouldn't bury things in
local BAD_SURFACE = { "water", "lava", "leaves", "chest", "barrel", "shulker", "sign", "bed", "door", "glass" }
local function badSurface(name)
  for _, s in ipairs(BAD_SURFACE) do
    if string.find(name, s, 1, true) then return true end
  end
  return false
end

-- find a spot one block under the surface somewhere around the player
local px, py, pz = position()
local tx, ty, tz, original
for _ = 1, 20 do
  local angle = math.random() * math.pi * 2
  local dist = MIN_DISTANCE + math.random() * (MAX_DISTANCE - MIN_DISTANCE)
  local x = math.floor(px + math.cos(angle) * dist)
  local z = math.floor(pz + math.sin(angle) * dist)

  -- walk down from above the player until we hit the ground
  local y = math.floor(py) + 30
  while y > math.floor(py) - 30 and isAir(block(x, y, z)) do
    y = y - 1
  end

  local surface = block(x, y, z)
  local below = block(x, y - 1, z)
  if surface and below and not isAir(surface) and not badSurface(surface)
      and not isAir(below) and not badSurface(below) then
    tx, ty, tz, original = x, y - 1, z, below
    break
  end
end

if not tx then
  error("couldn't find anywhere to bury the treasure, try somewhere with more solid ground", 0)
end

setBlock(tx, ty, tz, "gold_block")

local function distance()
  local x, y, z = position()
  local dx, dy, dz = x - (tx + 0.5), y - (ty + 0.5), z - (tz + 0.5)
  return math.sqrt(dx * dx + dy * dy + dz * dz)
end

local function heat(d)
  if d < 3 then return "<red><bold>BURNING</bold></red>"
  elseif d < 6 then return "<red>hot</red>"
  elseif d < 10 then return "<gold>warm</gold>"
  elseif d < 18 then return "<yellow>lukewarm</yellow>"
  elseif d < 28 then return "<aqua>cold</aqua>"
  else return "<blue><bold>freezing</bold></blue>" end
end

title("<gold><bold>TREASURE HUNT</bold></gold>", "<gray>a gold block is buried nearby, " .. TIME_LIMIT .. " seconds</gray>")
sound("block.amethyst_block.resonate", 1)
sleep(30)

local start = ticks()
local limit = TIME_LIMIT * 20
local nextClick = 0
local hinted = false
local found = false

while ticks() - start < limit do
  local elapsed = ticks() - start
  local left = math.ceil((limit - elapsed) / 20)

  -- dug up or broken by the player
  if block(tx, ty, tz) ~= "gold_block" then
    found = true
    break
  end

  local d = distance()
  actionbar(heat(d) .. " <dark_gray>|</dark_gray> <gray>" .. left .. "s</gray>")

  -- geiger counter: clicks faster and higher the closer you are
  if elapsed >= nextClick then
    local closeness = math.max(0, 1 - d / 40)
    sound("block.note_block.hat", 0.6 + closeness * 1.4, 0.5)
    nextClick = elapsed + math.max(1, math.floor(d / 3))
  end

  -- sparkles leak out of the ground when you're really close
  if d < 5 then
    particle("wax_on", tx + 0.5, ty + 1.2, tz + 0.5, 2, 0.4)
  end

  -- one hint: a beam of light over the spot
  if not hinted and left <= HINT_AT then
    hinted = true
    print("<yellow>Hint:</yellow> <gray>look for the beam of light!</gray>")
    sound("block.bell.use", 1)
    for h = 1, 40 do
      particle("end_rod", tx + 0.5, ty + h * 0.5, tz + 0.5, 1)
    end
  end

  -- last 10 seconds tick down loudly
  if left <= 10 and (limit - elapsed) % 20 == 0 then
    sound("block.note_block.pling", 0.5 + (10 - left) * 0.1, 1)
  end

  sleep(1)
end

if found then
  local seconds = math.floor((ticks() - start) / 20)
  local reward = REWARDS[math.random(#REWARDS)]
  give(reward.item, reward.count)
  title("<gold><bold>FOUND IT!</bold></gold>", "<gray>in " .. seconds .. " seconds, enjoy " .. reward.count .. "x " .. reward.item:gsub("_", " ") .. "</gray>")
  sound("ui.toast.challenge_complete", 1)
  particle("totem_of_undying", tx + 0.5, ty + 1, tz + 0.5, 150, 1, 0.5)
  particle("firework", tx + 0.5, ty + 1, tz + 0.5, 60, 0.5, 0.2)
else
  setBlock(tx, ty, tz, original)
  title("<red><bold>TIME'S UP</bold></red>", "<gray>it was at " .. tx .. ", " .. ty .. ", " .. tz .. "</gray>")
  sound("entity.villager.no", 1)
  for h = 1, 60 do
    particle("dust", tx + 0.5, ty + h * 0.5, tz + 0.5, 1, 0, 0, { color = "#ff3333", size = 2 })
  end
end
