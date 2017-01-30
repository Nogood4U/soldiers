let _server = {};
let players = {};
let bg;
let bulletsMap = {};
let hitEffects = [];
let worldSizeX = 2000;
let worldSizeY = 2000;
let sWorldSizeX = mtToPx(40);
let sWorldSizeY = mtToPx(40);
/*xMeters = 0.02f * xPixels;
 yMeters = 0.02f * yPixels;

 In reverse:

 xPixels = 50.0f * xMeters;
 yPixels = 50.0f * yMeters;*/

function pxToMt(unitPixel) {
    return unitPixel * 0.02;
}


function mtToPx(unitMeters) {
    return unitMeters * 50.0;
}

let game = {};

function createPlayer(_playerId, health) {
    let soldier = game.add.sprite(0, 0, 'soldierSelf');
    soldier.scale.setTo(3.125, 3.125);
    soldier.anchor.setTo(0.5, 0.5);
    soldier.animations.add("r", [17, 18, 19, 20, 21, 22]);
    soldier.animations.play('r', 9, true);
    soldier.nameTag = game.add.text(0, 0, _playerId, {font: "15px Arial", fill: "#ffffff"});
    soldier.nameTag.anchor.set(0.5);
    //healthBar
    soldier.healthBar = drawHealthBar(game, soldier, health);
    ///
    soldier.hitEffect = function (isHit, immune) {
        //play hit sound
        if (isHit) {
            hitEffects[Math.floor(Math.random() * 5)].play();
        }
        if (isHit || immune) {
            if (!soldier.hitTween) {
                soldier.hitTween = game.add.tween(soldier).to(
                    {
                        tint: 0xff0000
                    },
                    45,
                    Phaser.Easing.LINEAR,
                    true,
                    0,
                    -1,
                    true
                );
                // soldier.hitTween.yoyo(true, 0.32);
            }
        } else {
            soldier.tint = "0xffffff";
            if (soldier.hitTween) {
                soldier.hitTween.stop(true);
                soldier.hitTween = undefined;
            }
        }
    };
    //soldier.addChild(soldier.health)
    players[_playerId] = soldier;
    return soldier;
}

function initGame(playerId) {
    document.getElementById("formLogo").style.display = "none";
    let preload = function () {
        /////
        game.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;
        game.scale.refresh();
        //this.scale.pageAlignHorizontally = true;
        //this.scale.pageAlignVertically = true;
        /////
        game.load.spritesheet('soldierSelf', '/assets/images/sprites/soldierGun.png', 16, 16);
        game.load.spritesheet('weapons', '/assets/images/sprites/weapons.png', 24, 24);
        game.load.spritesheet('bullet', '/assets/images/sprites/bullet.png', 19, 19);
        game.load.image('background', '/assets/images/background/back_3.png');
        game.load.audio('map', ['assets/music/Mercury.mp3', 'assets/audio/Mercury.ogg']);
        game.load.audio('fire', ['assets/sounds/guns/laser.mp3', 'assets/sounds/guns/laser.ogg']);

        for (var i = 1; i < 7; i++) {
            game.load.audio('hit' + i, ['assets/sounds/hit/pain' + i + '.mp3', 'assets/sounds/hit/pain0' + i + '.ogg']);
        }

        _server = server("server", playerId);

    };

    let create = function () {
        bg = game.add.tileSprite(0, 0, sWorldSizeX, sWorldSizeY, 'background');
        weapon = game.add.sprite(game.width - 100, 0, 'weapons');
        weapon.scale.setTo(3.125, 3.125);
        weapon.frame = 0;
        weapon.fixedToCamera = true;
        score = game.add.text(game.width - 200, 0, "100%", {font: "32px Arial", fill: "#ffffff"});
        score.fixedToCamera = true;
        let soldierself = createPlayer(playerId, 100);
        cursors = game.input.keyboard.createCursorKeys();
        fireButton = game.input.keyboard.addKey(Phaser.Keyboard.SPACEBAR);
        shift = game.input.keyboard.addKey(Phaser.Keyboard.SHIFT);
        _server.then(obj => obj.stream.subscribe((val) => {
            val.players.forEach((serverPlayer) => {
                if (serverPlayer.playerId) {
                    let localPlayer = players[serverPlayer.playerId];
                    if (!localPlayer) {
                        localPlayer = createPlayer(serverPlayer.playerId, serverPlayer.health);
                    }
                    localPlayer.updated = true;
                    game.add.tween(localPlayer).to(
                        {
                            x: mtToPx(serverPlayer.posX),
                            y: mtToPx(serverPlayer.posY)
                        },
                        80,
                        Phaser.Easing.LINEAR,
                        true
                    );
                    game.add.tween(localPlayer.health).to(
                        {
                            x: mtToPx(serverPlayer.posX),
                            y: mtToPx(serverPlayer.posY) - localPlayer.height / 2
                        },
                        80,
                        Phaser.Easing.LINEAR,
                        true
                    );
                    game.add.tween(localPlayer.nameTag).to(
                        {
                            x: mtToPx(serverPlayer.posX),
                            y: mtToPx(serverPlayer.posY) - localPlayer.height / 2 - 15
                        },
                        80,
                        Phaser.Easing.LINEAR,
                        true
                    );
                    //healthbar redrwa ... performance hit ????
                    localPlayer.healthBar = drawHealthBar(game, localPlayer, serverPlayer.health, localPlayer.healthBar);
                    //localPlayer.healthBar.bar.dirty = true;
                    game.add.tween(localPlayer.healthBar.sprite).to(
                        {
                            x: mtToPx(serverPlayer.posX) - (localPlayer.width / 2) * (localPlayer.scale.x / Math.abs(localPlayer.scale.x)),
                            y: mtToPx(serverPlayer.posY) - localPlayer.height / 2 - 5
                        },
                        80,
                        Phaser.Easing.LINEAR,
                        true
                    );
                    //
                    //setPlayerorientation
                    if (!serverPlayer.viewOr && localPlayer.scale.x > 0)
                        localPlayer.scale.x *= -1;
                    else if (serverPlayer.viewOr && localPlayer.scale.x < 0)
                        localPlayer.scale.x *= -1;
                    //Set Player Weapon
                    if (serverPlayer.playerId === playerId) {
                        score.text = serverPlayer.health + "%";
                        switch (serverPlayer.currWpn) {
                            case 1:
                                weapon.frame = 0;
                                break;
                            case 2:
                                weapon.frame = 6;
                                break;
                            case 3:
                                weapon.frame = 9;
                                break;
                        }
                    }

                    //Set Player visual and sound if hit
                    localPlayer.hitEffect(serverPlayer.hit, serverPlayer.hitImmune);
                }
            });
            val.bullets.forEach((serverPlayer) => {
                if (serverPlayer.ownerId) {
                    if (bulletsMap[serverPlayer.bulletNum]) {
                        game.add.tween(bulletsMap[serverPlayer.bulletNum]).to(
                            {
                                x: mtToPx(serverPlayer.posX),
                                y: mtToPx(serverPlayer.posY)
                            },
                            60,
                            Phaser.Easing.LINEAR,
                            true
                        );
                        /* bulletsMap[serverPlayer.bulletNum].x = mtToPx(serverPlayer.posX);
                         bulletsMap[serverPlayer.bulletNum].y = mtToPx(serverPlayer.posY);*/
                    } else {
                        let bullet = bullets.getFirstExists(false);
                        if (bullet) {
                            //  Grab the first bullet we can from the pool
                            bullet.reset(mtToPx(serverPlayer.posX), mtToPx(serverPlayer.posY));
                            bullet.x = mtToPx(serverPlayer.posX);
                            bullet.y = mtToPx(serverPlayer.posY);
                            bulletsMap[serverPlayer.bulletNum] = bullet;
                        }
                    }
                    bulletsMap[serverPlayer.bulletNum].updated = true;
                }
            });
            Object.entries(bulletsMap).forEach(([key, bullet]) => {
                if (!bullet.updated) {
                    bullet.kill();
                    delete bulletsMap[key];
                } else {
                    bullet.updated = false;
                }
            });
            Object.entries(players).forEach(([key, player]) => {
                if (!player.updated) {
                    player.destroy();
                    player.healthBar.sprite.destroy();
                    delete players[player];
                    player.nameTag.destroy();
                } else {
                    player.updated = false;
                }
            });
        }));
        game.world.setBounds(0, 0, sWorldSizeX, sWorldSizeY);
        game.camera.follow(soldierself);
        game.time.advancedTiming = true;
        game.input.keyboard.onDownCallback = function () {
            let commands = {
                "xMv": 0,
                "yMv": 0,
                "weapon": 0,
                "viewOr": 0,
                "btns": []
            };
            let send = false;
            let btns = [];
            if (game.input.keyboard.event.keyCode == Phaser.Keyboard.SHIFT) {
                btns.push("shift");
                send = true;
            }
            if (game.input.keyboard.event.keyCode == Phaser.Keyboard.SPACEBAR) {
                btns.push("fire");
                send = true;
                gunFire.play();
            }
            commands.btns = btns;
            _server.then(obj => {
                if (obj.ws.readyState === 1 && send)
                    obj.ws.sendX(JSON.stringify(commands));
            });
        };
        //  Our bullet group
        bullets = game.add.group();
        bullets.createMultiple(100, 'bullet');
        bullets.setAll('anchor.x', 0.5);
        bullets.setAll('anchor.y', 0.5);
        music = game.add.audio('map');
        gunFire = game.add.audio('fire');
        gunFire.volume = 0.3;
        music.loopFull(0.1);
        for (var i = 1; i < 7; i++) {
            hitEffects.push(game.add.audio('hit' + i));
        }
    };

    let update = function () {
        /* bg.tilePosition.x += 0.5;
         bg.tilePosition.y += 0.5;*/
        let btns = [];
        let commands = {
            "xMv": 0,
            "yMv": 0,
            "weapon": 0,
            "viewOr": 1
        };
        let send = false;
        if (cursors.up.isDown) {
            commands.yMv = -10;
            send = true;
        }
        else if (cursors.down.isDown) {
            commands.yMv = 10;
            send = true;
        }
        if (cursors.left.isDown) {
            commands.xMv = -10;
            send = true;
        }
        else if (cursors.right.isDown) {
            commands.xMv = 10;
            send = true;
        }
        commands.btns = btns;
        _server.then(obj => {
            if (obj.ws.readyState === 1 && send)
                obj.ws.sendX(JSON.stringify(commands));
        });
        // game.debug.cameraInfo(game.camera, 32, 32);
        game.debug.spriteInfo(players[playerId], 32, 32);
        game.debug.text(game.time.fps || '--', 13, 200, "#00ff00");
    };
    game = new Phaser.Game(window.innerWidth * window.devicePixelRatio, window.innerHeight * window.devicePixelRatio, Phaser.AUTO, 'gameCanvas', {
        preload: preload,
        create: create,
        update: update
    });

}

function drawHealthBar(game, sprite, health, healthBar) {
    let hBar;
    let barX = 50;
    let barY = 5;

    if (healthBar) {
        hBar = healthBar.bar;
        hBar.clear();
    } else {
        hBar = game.add.bitmapData(barX, barY);

    }
    hBar.ctx.beginPath();
    hBar.ctx.rect(0, 0, barX, barY);
    hBar.ctx.fillStyle = '#a60400';
    // hBar.ctx.strokeStyle = '#18a607';
    hBar.ctx.fill();
    let HbarSpr;
    if (healthBar && healthBar.sprite) {
        HbarSpr = healthBar.sprite;
    } else
        HbarSpr = game.add.sprite(sprite.x, sprite.y - 15, hBar);

    let hBarMask;
    if (healthBar && healthBar.hpMask) {
        hBarMask = healthBar.hpMask
    } else {
        hBarMask = game.add.bitmapData(barX, barY);

    }
    hBarMask.ctx.beginPath();
    if (healthBar && health !== healthBar.currHealth) {
        console.log("redraw...", barX - (barX * (health / 100)));
        hBarMask.clear();
        hBarMask.ctx.rect(0, 0, (barX * (health / 100)), barY);
        hBarMask.ctx.fillStyle = '#18a607';
        hBarMask.ctx.fill();
    } else if (healthBar) {
        hBarMask.clear();
        hBarMask.ctx.rect(0, 0, barX * (health / 100), barY);
        hBarMask.ctx.fillStyle = '#18a607';
        hBarMask.ctx.fill();
    }
    hBar.alphaMask(hBar, hBarMask);
    let hBarStroke;
    if (healthBar && healthBar.stroke) {
        hBarStroke = healthBar.stroke
    } else {
        hBarStroke = game.add.bitmapData(barX, barY);
        hBarStroke.ctx.strokeStyle = '#cfcfcf';
        hBarStroke.ctx.rect(0, 0, barX, barY);
        hBarStroke.ctx.stroke();
    }
    hBar.alphaMask(hBar, hBarStroke);
    hBarStroke.dirty = true;
    hBar.dirty = true;
    hBarMask.dirty = true;
    return {
        sprite: HbarSpr,
        bar: hBar,
        hpMask: hBarMask,
        stroke: hBarStroke,
        currHealth: health
    };
}

